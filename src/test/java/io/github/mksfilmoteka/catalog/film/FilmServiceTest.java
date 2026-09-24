package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.actor.ActorService;
import io.github.mksfilmoteka.catalog.common.PageResponse;
import io.github.mksfilmoteka.catalog.common.exception.BadRequestException;
import io.github.mksfilmoteka.catalog.common.exception.ConflictException;
import io.github.mksfilmoteka.catalog.common.exception.ResourceNotFoundException;
import io.github.mksfilmoteka.catalog.director.DirectorService;
import io.github.mksfilmoteka.catalog.film.dto.*;
import io.github.mksfilmoteka.catalog.outbox.OutboxEvent;
import io.github.mksfilmoteka.catalog.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.github.mksfilmoteka.catalog.actor.ActorTestData.actorRequest;
import static io.github.mksfilmoteka.catalog.actor.ActorTestData.loadedActor;
import static io.github.mksfilmoteka.catalog.director.DirectorTestData.directorRequest;
import static io.github.mksfilmoteka.catalog.director.DirectorTestData.loadedDirector;
import static io.github.mksfilmoteka.catalog.film.FilmTestData.*;
import static io.github.mksfilmoteka.catalog.util.TestUtil.JSON_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmServiceTest {

    @Mock
    private FilmRepository filmRepository;

    @Mock
    private FilmMapper filmMapper;

    @Mock
    private ActorService actorService;

    @Mock
    private DirectorService directorService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private FilmService filmService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filmService, "filmDeletedTopic", FILM_DELETED_TOPIC);
        ReflectionTestUtils.setField(filmService, "filmPosterChangedTopic", FILM_POSTER_CHANGED_TOPIC);
    }

    @Test
    void shouldCreateFilm() {
        Film film = film();
        Film loadedFilm = loadedFilm();
        loadedFilm.addActor(loadedActor());
        loadedFilm.addDirector(loadedDirector());

        when(filmMapper.filmRequestToFilm(filmRequestFull())).thenReturn(film);
        when(actorService.findOrCreate(actorRequest())).thenReturn(loadedActor());
        when(directorService.findOrCreate(directorRequest())).thenReturn(loadedDirector());
        when(filmMapper.filmToDetailedFilmResponse(loadedFilm)).thenReturn(detailedFilmResponseFull());
        when(filmRepository.save(any(Film.class))).thenReturn(loadedFilm);

        DetailedFilmResponse response = filmService.createFilm(filmRequestFull());

        assertThat(response).isEqualTo(detailedFilmResponseFull());

        verify(filmMapper).filmRequestToFilm(filmRequestFull());
        verify(actorService).findOrCreate(actorRequest());
        verify(directorService).findOrCreate(directorRequest());
        verify(filmRepository).save(film);
        verify(filmMapper).filmToDetailedFilmResponse(loadedFilm);
    }

    @Test
    void shouldCreateFilmWithDistinctActorsAndDirectors() {
        Film film = film();
        Film loadedFilm = loadedFilm();
        var actor = loadedActor();
        var director = loadedDirector();
        FilmRequest request = filmRequestWithDuplications();

        when(filmMapper.filmRequestToFilm(request)).thenReturn(film);
        when(actorService.findOrCreate(actorRequest())).thenReturn(actor);
        when(directorService.findOrCreate(directorRequest())).thenReturn(director);
        when(filmMapper.filmToDetailedFilmResponse(loadedFilm)).thenReturn(detailedFilmResponseFull());
        when(filmRepository.save(film)).thenReturn(loadedFilm);

        DetailedFilmResponse response = filmService.createFilm(request);

        assertThat(response).isEqualTo(detailedFilmResponseFull());
        assertThat(film.getActors()).containsExactly(actor);
        assertThat(film.getDirectors()).containsExactly(director);
        verify(actorService, times(1)).findOrCreate(actorRequest());
        verify(directorService, times(1)).findOrCreate(directorRequest());
    }

    @Test
    void shouldThrowOnCreateIfConflict() {
        when(filmRepository.existsByTitleAndReleaseYear(anyString(), anyInt())).thenReturn(true);
        FilmRequest request = filmRequestFull();
        assertThrows(ConflictException.class, () -> filmService.createFilm(request));

        verify(filmRepository).existsByTitleAndReleaseYear(anyString(), anyInt());
        verifyNoInteractions(filmMapper);
    }

    @Test
    void shouldFindFilmByIdIfExists() {
        Film loadedFilm = loadedFilm();

        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.of(loadedFilm));
        when(filmMapper.filmToDetailedFilmResponse(loadedFilm)).thenReturn(detailedFilmResponseFull());

        DetailedFilmResponse response = filmService.findById(FILM_ID);

        assertThat(response).isEqualTo(detailedFilmResponseFull());
        verify(filmRepository).findById(FILM_ID);
        verify(filmMapper).filmToDetailedFilmResponse(loadedFilm);
    }

    @Test
    void shouldReturnMissingFilmIds() {
        Set<Long> requestedFilmIds = Set.of(1L, 2L, 3L);

        when(filmRepository.findExistingFilmIds(requestedFilmIds)).thenReturn(Set.of(1L, 3L));
        FilmExistenceResponse response = filmService.checkFilmExistence(new FilmExistenceRequest(requestedFilmIds));

        assertThat(response.missingFilmIds()).containsExactlyInAnyOrder(2L);
        verify(filmRepository).findExistingFilmIds(requestedFilmIds);
    }

    @Test
    void shouldReturnEmptyMissingFilmIdsWhenAllFilmsExist() {
        Set<Long> requestedFilmIds = Set.of(1L, 2L, 3L);

        when(filmRepository.findExistingFilmIds(requestedFilmIds)).thenReturn(requestedFilmIds);
        FilmExistenceResponse response = filmService.checkFilmExistence(new FilmExistenceRequest(requestedFilmIds));

        assertThat(response.missingFilmIds()).isEmpty();
        verify(filmRepository).findExistingFilmIds(requestedFilmIds);
    }

    @Test
    void shouldThrowIfDoesNotExist() {
        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> filmService.findById(FILM_ID));

        verify(filmRepository).findById(FILM_ID);
        verifyNoInteractions(filmMapper);
    }

    @Test
    void shouldReturnPagedFilmsUnfiltered() {
        List<Film> films = List.of(loadedFilm());
        Pageable pageable = PageRequest.of(0, 100);
        Page<Film> page = new PageImpl<>(films, pageable, films.size());
        when(filmRepository.findAll(ArgumentMatchers.<Specification<Film>>any(), eq(pageable))).thenReturn(page);
        when(filmMapper.filmsToFilmResponses(page.getContent())).thenReturn(List.of(filmResponse()));

        PageResponse<FilmResponse> response = filmService.getFilms(emptyFilmFilter(), pageable);

        assertThat(response.content()).containsExactly(filmResponse());
        assertThat(response.size()).isEqualTo(100);
        assertThat(response.totalElements()).isEqualTo(1);

        verify(filmRepository).findAll(ArgumentMatchers.<Specification<Film>>any(), eq(pageable));
        verify(filmMapper).filmsToFilmResponses(films);
    }

    @Test
    void shouldReturnFilmsFiltered() {
        List<Film> films = List.of(loadedFilm());
        Page<Film> page = new PageImpl<>(films);
        Pageable pageable = PageRequest.of(0, 100);
        when(filmRepository.findAll(ArgumentMatchers.<Specification<Film>>any(), eq(pageable))).thenReturn(page);
        when(filmMapper.filmsToFilmResponses(page.getContent())).thenReturn(List.of(filmResponse()));

        PageResponse<FilmResponse> response = filmService.getFilms(filmFilter(), pageable);

        assertThat(response.content()).containsExactly(filmResponse());
        assertThat(response.totalElements()).isEqualTo(1);

        verify(filmRepository).findAll(ArgumentMatchers.<Specification<Film>>any(), eq(pageable));
        verify(filmMapper).filmsToFilmResponses(films);
    }

    @Test
    void shouldReturnFilmCollectionFiltered() {
        List<Film> films = List.of(loadedFilm());
        Page<Film> page = new PageImpl<>(films);
        Pageable pageable = PageRequest.of(0, 100);
        when(filmRepository.findAll(ArgumentMatchers.<Specification<Film>>any(), eq(pageable))).thenReturn(page);
        when(filmMapper.filmsToFilmResponses(page.getContent())).thenReturn(List.of(filmResponse()));

        PageResponse<FilmResponse> response = filmService.getFilmCollection(filmCollectionFilter(), pageable);

        assertThat(response.content()).containsExactly(filmResponse());
        assertThat(response.totalElements()).isEqualTo(1);

        verify(filmRepository).findAll(ArgumentMatchers.<Specification<Film>>any(), eq(pageable));
        verify(filmMapper).filmsToFilmResponses(films);
    }

    @Test
    void shouldReturnEmptyListIfNotExist() {
        List<Film> films = List.of();
        Page<Film> page = new PageImpl<>(films);
        Pageable pageable = PageRequest.of(0, 100);
        when(filmRepository.findAll(ArgumentMatchers.<Specification<Film>>any(), eq(pageable))).thenReturn(page);
        when(filmMapper.filmsToFilmResponses(List.of())).thenReturn(List.of());

        PageResponse<FilmResponse> response = filmService.getFilms(emptyFilmFilter(), pageable);

        assertThat(response.content()).isEmpty();

        verify(filmRepository).findAll(ArgumentMatchers.<Specification<Film>>any(), eq(pageable));
        verify(filmMapper).filmsToFilmResponses(List.of());
    }

    @Test
    void shouldThrowForUnsupportedSortField() {
        Pageable pageable = PageRequest.of(0, 100, Sort.by("test"));
        FilmFilter filter = emptyFilmFilter();
        assertThrows(BadRequestException.class, () -> filmService.getFilms(filter, pageable));

        verifyNoInteractions(filmRepository);
    }

    @Test
    void shouldThrowForUnsupportedSortFieldInFilmCollection() {
        Pageable pageable = PageRequest.of(0, 100, Sort.by("test"));
        FilmFilter filter = filmCollectionFilter();
        assertThrows(BadRequestException.class, () -> filmService.getFilmCollection(filter, pageable));

        verifyNoInteractions(filmRepository);
    }

    @Test
    void shouldThrowOnUpdateIfTitleAndReleaseYearAlreadyExist() {
        Film loadedFilm = loadedFilm();
        loadedFilm.setTitle("old title");
        FilmRequest request = filmRequestFull();

        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.of(loadedFilm));
        when(filmRepository.existsByTitleAndReleaseYear(FILM_TITLE, RELEASE_YEAR)).thenReturn(true);

        assertThrows(ConflictException.class, () -> filmService.updateFilm(FILM_ID, request));

        verify(filmRepository).existsByTitleAndReleaseYear(FILM_TITLE, RELEASE_YEAR);
        verify(filmRepository, never()).saveAndFlush(any());
        verifyNoInteractions(filmMapper, actorService, directorService);
    }

    @Test
    void shouldUpdateFilmIfExists() {
        Film loadedFilm = loadedFilm();
        loadedFilm.setTitle("old title");
        FilmRequest request = filmRequestWithDuplications();

        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.of(loadedFilm));
        when(actorService.findOrCreate(any())).thenReturn(loadedActor());
        when(directorService.findOrCreate(any())).thenReturn(loadedDirector());

        doAnswer(updateTitleOnly()).when(filmMapper).updateFilmRequestToFilm(any(), any());

        when(filmRepository.saveAndFlush(any(Film.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(filmMapper.filmToDetailedFilmResponse(any(Film.class))).thenReturn(detailedFilmResponseFull());

        DetailedFilmResponse response = filmService.updateFilm(FILM_ID, request);

        assertThat(response).isEqualTo(detailedFilmResponseFull());
        ArgumentCaptor<Film> captor = ArgumentCaptor.forClass(Film.class);

        verify(filmRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo(FILM_TITLE);
        verify(filmMapper).updateFilmRequestToFilm(request, loadedFilm);
        verify(actorService, times(1)).findOrCreate(actorRequest());
        verify(directorService, times(1)).findOrCreate(directorRequest());
        verify(filmMapper).filmToDetailedFilmResponse(any(Film.class));
        verifyNoInteractions(outboxEventRepository, jsonMapper);
    }

    @Test
    void shouldSaveFilmPosterChangedEventWhenPosterChanges() {
        Film film = loadedFilm();
        String oldPosterName = "old-poster.jpg";
        film.setPosterName(oldPosterName);
        FilmRequest request = filmRequestFull();

        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.of(film));
        when(actorService.findOrCreate(actorRequest())).thenReturn(loadedActor());
        when(directorService.findOrCreate(directorRequest())).thenReturn(loadedDirector());
        doAnswer(invocation -> {
            FilmRequest update = invocation.getArgument(0);
            Film target = invocation.getArgument(1);
            target.setPosterName(update.posterName());
            return null;
        }).when(filmMapper).updateFilmRequestToFilm(request, film);
        when(filmRepository.saveAndFlush(film)).thenReturn(film);
        Instant beforeUpdate = Instant.now();

        filmService.updateFilm(FILM_ID, request);

        Instant afterUpdate = Instant.now();
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent outboxEvent = captor.getValue();
        assertThat(outboxEvent.getId()).isNotNull();
        assertThat(outboxEvent.getTopic()).isEqualTo(FILM_POSTER_CHANGED_TOPIC);
        assertThat(outboxEvent.getMessageKey()).isEqualTo(Long.toString(FILM_ID));
        assertThat(outboxEvent.getCreatedTs()).isBetween(beforeUpdate, afterUpdate);
        assertThat(outboxEvent.getPublishedTs()).isNull();

        FilmPosterChangedEvent event = JSON_MAPPER.readValue(outboxEvent.getPayload(), FilmPosterChangedEvent.class);
        assertThat(event.eventId()).isEqualTo(outboxEvent.getId());
        assertThat(event.filmId()).isEqualTo(FILM_ID);
        assertThat(event.oldPosterName()).isEqualTo(oldPosterName);
        assertThat(event.newPosterName()).isEqualTo(FILM_POSTER_NAME);
        assertThat(event.occurredAt()).isEqualTo(outboxEvent.getCreatedTs());
    }

    @Test
    void shouldRejectStaleUpdateWithoutChangingPosterOrCreatingCleanupEvent() {
        Film film = loadedFilm();
        film.setVersion(1L);
        film.setPosterName("current-poster.jpg");

        FilmRequest staleRequest = filmRequestFull();
        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.of(film));

        assertThrows(ConflictException.class, () -> filmService.updateFilm(FILM_ID, staleRequest));
        assertThat(film.getPosterName()).isEqualTo("current-poster.jpg");
        verify(filmRepository, never()).saveAndFlush(any());
        verifyNoInteractions(filmMapper, actorService, directorService, outboxEventRepository, jsonMapper);
    }

    @Test
    void shouldSaveFilmDeletedEventAndDeleteFilm() {
        Film film = loadedFilm();
        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.of(film));
        Instant beforeDeletion = Instant.now();

        filmService.deleteFilm(FILM_ID);

        Instant afterDeletion = Instant.now();
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        verify(filmRepository).delete(film);

        OutboxEvent outboxEvent = captor.getValue();
        assertThat(outboxEvent.getId()).isNotNull();
        assertThat(outboxEvent.getTopic()).isEqualTo(FILM_DELETED_TOPIC);
        assertThat(outboxEvent.getMessageKey()).isEqualTo(Long.toString(FILM_ID));
        assertThat(outboxEvent.getCreatedTs()).isBetween(beforeDeletion, afterDeletion);
        assertThat(outboxEvent.getPublishedTs()).isNull();

        FilmDeletedEvent event = JSON_MAPPER.readValue(outboxEvent.getPayload(), FilmDeletedEvent.class);
        assertThat(event.eventId()).isEqualTo(outboxEvent.getId());
        assertThat(event.filmId()).isEqualTo(FILM_ID);
        assertThat(event.posterName()).isEqualTo(FILM_POSTER_NAME);
        assertThat(event.occurredAt()).isEqualTo(outboxEvent.getCreatedTs());
    }

    @Test
    void shouldThrowOnDeleteIfNotExists() {
        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> filmService.deleteFilm(FILM_ID));

        verify(filmRepository, never()).delete(any(Film.class));
        verifyNoInteractions(outboxEventRepository, jsonMapper);
    }

    @Test
    void shouldNotDeleteFilmWhenEventSerializationFails() {
        Film film = loadedFilm();
        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.of(film));
        JacksonException exception = mock(JacksonException.class);
        doThrow(exception).when(jsonMapper).writeValueAsString(any(FilmDeletedEvent.class));

        assertThrows(JacksonException.class, () -> filmService.deleteFilm(FILM_ID));

        verify(filmRepository, never()).delete(any(Film.class));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldNotDeleteFilmWhenSavingOutboxEventFails() {
        Film film = loadedFilm();
        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.of(film));
        var exception = new DataAccessResourceFailureException("Outbox is unavailable");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenThrow(exception);

        assertThrows(DataAccessResourceFailureException.class, () -> filmService.deleteFilm(FILM_ID));

        verify(filmRepository, never()).delete(any(Film.class));
    }

    private static Answer<Void> updateTitleOnly() {
        return invocation -> {
            FilmRequest request = invocation.getArgument(0);
            Film film = invocation.getArgument(1);
            film.setTitle(request.title());
            return null;
        };
    }
}
