package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.actor.ActorService;
import io.github.mksfilmoteka.catalog.common.PageResponse;
import io.github.mksfilmoteka.catalog.common.exception.BadRequestException;
import io.github.mksfilmoteka.catalog.common.exception.ConflictException;
import io.github.mksfilmoteka.catalog.common.exception.ResourceNotFoundException;
import io.github.mksfilmoteka.catalog.director.DirectorService;
import io.github.mksfilmoteka.catalog.film.dto.DetailedFilmResponse;
import io.github.mksfilmoteka.catalog.film.dto.FilmFilter;
import io.github.mksfilmoteka.catalog.film.dto.FilmRequest;
import io.github.mksfilmoteka.catalog.film.dto.FilmResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static io.github.mksfilmoteka.catalog.actor.ActorTestData.actorRequest;
import static io.github.mksfilmoteka.catalog.actor.ActorTestData.loadedActor;
import static io.github.mksfilmoteka.catalog.director.DirectorTestData.directorRequest;
import static io.github.mksfilmoteka.catalog.director.DirectorTestData.loadedDirector;
import static io.github.mksfilmoteka.catalog.film.FilmTestData.*;
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

    @InjectMocks
    private FilmService filmService;

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
        verify(filmRepository, never()).save(any());
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

        when(filmRepository.save(any(Film.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(filmMapper.filmToDetailedFilmResponse(any(Film.class))).thenReturn(detailedFilmResponseFull());

        DetailedFilmResponse response = filmService.updateFilm(FILM_ID, request);

        assertThat(response).isEqualTo(detailedFilmResponseFull());
        ArgumentCaptor<Film> captor = ArgumentCaptor.forClass(Film.class);

        verify(filmRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo(FILM_TITLE);
        verify(filmMapper).updateFilmRequestToFilm(request, loadedFilm);
        verify(actorService, times(1)).findOrCreate(actorRequest());
        verify(directorService, times(1)).findOrCreate(directorRequest());
        verify(filmMapper).filmToDetailedFilmResponse(any(Film.class));
    }

    @Test
    void shouldDeleteFilm() {
        Film film = loadedFilm();
        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.of(film));
        filmService.deleteFilm(FILM_ID);

        verify(filmRepository).delete(film);
    }

    @Test
    void shouldThrowOnDeleteIfNotExists() {
        when(filmRepository.findById(FILM_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> filmService.deleteFilm(FILM_ID));

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
