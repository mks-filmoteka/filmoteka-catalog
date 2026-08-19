package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.auth.KeycloakRealmRoleConverter;
import io.github.mksfilmoteka.catalog.auth.SecurityConfig;
import io.github.mksfilmoteka.catalog.common.PageResponse;
import io.github.mksfilmoteka.catalog.common.exception.BadRequestException;
import io.github.mksfilmoteka.catalog.common.exception.ConflictException;
import io.github.mksfilmoteka.catalog.common.exception.ErrorCode;
import io.github.mksfilmoteka.catalog.common.exception.ResourceNotFoundException;
import io.github.mksfilmoteka.catalog.film.dto.FilmFilter;
import io.github.mksfilmoteka.catalog.film.dto.FilmRequest;
import io.github.mksfilmoteka.catalog.film.dto.FilmResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static io.github.mksfilmoteka.catalog.actor.ActorTestData.ACTOR_NAME;
import static io.github.mksfilmoteka.catalog.director.DirectorTestData.DIRECTOR_NAME;
import static io.github.mksfilmoteka.catalog.film.FilmTestData.*;
import static io.github.mksfilmoteka.catalog.util.TestUtil.JSON_MAPPER;
import static io.github.mksfilmoteka.catalog.util.TestUtil.adminJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilmController.class)
@Import({SecurityConfig.class, KeycloakRealmRoleConverter.class})
class FilmControllerTest {

    @MockitoBean
    private FilmService filmService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateFilm() throws Exception {
        when(filmService.createFilm(any(FilmRequest.class))).thenReturn(detailedFilmResponseFull());

        mockMvc.perform(post("/api/v1/films")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_MAPPER.writeValueAsString(filmRequestFull()))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(FILM_ID))
                .andExpect(jsonPath("$.title").value(FILM_TITLE))
                .andExpect(jsonPath("$.releaseYear").value(RELEASE_YEAR))
                .andExpect(jsonPath("$.countries[0]").value(Country.UNITED_STATES.getJsonValue()))
                .andExpect(jsonPath("$.description").value(FILM_DESCRIPTION))
                .andExpect(jsonPath("$.posterName").value(FILM_POSTER_NAME))
                .andExpect(jsonPath("$.actors[0].name").value(ACTOR_NAME))
                .andExpect(jsonPath("$.directors[0].name").value(DIRECTOR_NAME));

        verify(filmService).createFilm(any(FilmRequest.class));
    }

    @Test
    void shouldThrowOnCreateIfConflict() throws Exception {
        String message = String.format("Film with title '%s' and release year '%s' already exists",
                FILM_TITLE, RELEASE_YEAR);
        when(filmService.createFilm(any(FilmRequest.class))).thenThrow(new ConflictException(message));

        mockMvc.perform(post("/api/v1/films")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_MAPPER.writeValueAsString(filmRequestFull()))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.code").value(ErrorCode.CONFLICT.name()));

    }

    @Test
    void shouldThrowOnCreateIfInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/films")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_MAPPER.writeValueAsString(invalidFilmRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.name()))
                .andExpect(jsonPath("$.errorDetails[*].field",
                        containsInAnyOrder(
                                "title",
                                "releaseYear",
                                "countries",
                                "description",
                                "posterName",
                                "genres",
                                "actors",
                                "directors")));
    }

    @Test
    void shouldThrowOnCreateIfCountryIsUnknown() throws Exception {
        mockMvc.perform(post("/api/v1/films")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmRequestJson("[\"Atlantis\"]", "[\"Action\"]"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.message").value("Invalid value 'Atlantis' for field 'countries'"))
                .andExpect(jsonPath("$.errorDetails[0].field").value("countries"))
                .andExpect(jsonPath("$.errorDetails[0].message").value(containsString("UNITED_STATES")));

        verify(filmService, never()).createFilm(any(FilmRequest.class));
    }

    @Test
    void shouldThrowOnCreateIfGenreIsUnknown() throws Exception {
        mockMvc.perform(post("/api/v1/films")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmRequestJson("[\"United States\"]", "[\"Bad Genre\"]"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.message").value("Invalid value 'Bad Genre' for field 'genres'"))
                .andExpect(jsonPath("$.errorDetails[0].field").value("genres"))
                .andExpect(jsonPath("$.errorDetails[0].message").value(containsString("ACTION")));

        verify(filmService, never()).createFilm(any(FilmRequest.class));
    }

    @Test
    void shouldFindFilmById() throws Exception {
        when(filmService.findById(FILM_ID)).thenReturn(detailedFilmResponseFull());

        mockMvc.perform(get("/api/v1/films/{id}", FILM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(FILM_ID))
                .andExpect(jsonPath("$.title").value(FILM_TITLE))
                .andExpect(jsonPath("$.releaseYear").value(RELEASE_YEAR));

        verify(filmService).findById(FILM_ID);
    }

    @Test
    void shouldThrowIfFilmNotFound() throws Exception {
        String message = "Film with id " + FILM_ID + " not found";
        when(filmService.findById(FILM_ID)).thenThrow(new ResourceNotFoundException(message));

        mockMvc.perform(get("/api/v1/films/{id}", FILM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.name()));
    }

    @Test
    void shouldThrowWithDetailsIfFilmIdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/films/{id}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.errorDetails[0].field").value("id"))
                .andExpect(jsonPath("$.errorDetails[0].message").value("Expected type: Long"));
    }

    @Test
    void shouldReturnPagedFilmsUnfiltered() throws Exception {
        PageResponse<FilmResponse> response =
                new PageResponse<>(List.of(filmResponse()), 0, 100, 1, 1);
        when(filmService.getFilms(eq(emptyFilmFilter()), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content[0].title").value(FILM_TITLE))
                .andExpect(jsonPath("$.content[0].id").value(FILM_ID));

        verify(filmService).getFilms(eq(emptyFilmFilter()), any());
    }

    @Test
    void shouldReturnPagedEmptyList() throws Exception {
        PageResponse<FilmResponse> response =
                new PageResponse<>(List.of(), 0, 100, 0, 1);
        when(filmService.getFilms(any(FilmFilter.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(filmService).getFilms(any(FilmFilter.class), any());
    }

    @Test
    void shouldReturnPagedFilmsFiltered() throws Exception {
        PageResponse<FilmResponse> response =
                new PageResponse<>(List.of(filmResponse()), 0, 100, 1, 1);
        when(filmService.getFilms(eq(filmFilter()), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/films")
                        .param("title", "film title")
                        .param("yearFrom", "2000")
                        .param("yearTo", "2010")
                        .param("countries", "UNITED_STATES", "ITALY")
                        .param("genres", "ACTION", "ADVENTURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(FILM_TITLE));

        verify(filmService).getFilms(any(FilmFilter.class), any());
    }

    @Test
    void shouldReturnPagedFilmCollectionFiltered() throws Exception {
        PageResponse<FilmResponse> response =
                new PageResponse<>(List.of(filmResponse()), 0, 100, 1, 1);
        when(filmService.getFilmCollection(eq(filmCollectionFilter()), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/films/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_MAPPER.writeValueAsString(filmCollectionFilter())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(FILM_TITLE));

        verify(filmService).getFilmCollection(eq(filmCollectionFilter()), any());
    }

    @Test
    void shouldThrowOnFilmCollectionIfIdsAreMissing() throws Exception {
        mockMvc.perform(post("/api/v1/films/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.message").value("Film ids are required for collection search"));

        verify(filmService, never()).getFilmCollection(any(), any());
    }

    @Test
    void shouldUseFixedPageSizeAndAppendIdSortForFilmCollection() throws Exception {
        PageResponse<FilmResponse> response =
                new PageResponse<>(List.of(), 2, 100, 0, 0);
        when(filmService.getFilmCollection(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/films/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_MAPPER.writeValueAsString(emptyFilmCollectionFilter()))
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "title,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(filmService).getFilmCollection(any(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        Sort.Order titleOrder = pageable.getSort().getOrderFor("title");
        Sort.Order idOrder = pageable.getSort().getOrderFor("id");

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(titleOrder).isNotNull();
        assertThat(titleOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(idOrder).isNotNull();
        assertThat(idOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void shouldUseFixedPageSizeAndAppendIdSort() throws Exception {
        PageResponse<FilmResponse> response =
                new PageResponse<>(List.of(), 2, 100, 0, 0);
        when(filmService.getFilms(any(FilmFilter.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/films")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "title,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(filmService).getFilms(any(FilmFilter.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        Sort.Order titleOrder = pageable.getSort().getOrderFor("title");
        Sort.Order idOrder = pageable.getSort().getOrderFor("id");

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(titleOrder).isNotNull();
        assertThat(titleOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(idOrder).isNotNull();
        assertThat(idOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void shouldThrowForUnsupportedSortField() throws Exception {
        when(filmService.getFilms(any(FilmFilter.class), any(Pageable.class)))
                .thenThrow(new BadRequestException("Unsupported sort field: test"));

        mockMvc.perform(get("/api/v1/films")
                        .param("sort", "test,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.message").value("Unsupported sort field: test"));
    }

    @Test
    void shouldUpdateFilm() throws Exception {
        when(filmService.updateFilm(eq(FILM_ID), any(FilmRequest.class))).thenReturn(detailedFilmResponseFull());

        mockMvc.perform(
                        put("/api/v1/films/{id}", FILM_ID)
                                .with(adminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON_MAPPER.writeValueAsString(filmRequestFull()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(FILM_ID))
                .andExpect(jsonPath("$.title").value(FILM_TITLE));

        verify(filmService).updateFilm(eq(FILM_ID), any(FilmRequest.class));
    }

    @Test
    void shouldThrowOnUpdateIfConflict() throws Exception {
        String message = String.format("Film with title '%s' and release year '%s' already exists",
                FILM_TITLE, RELEASE_YEAR);
        when(filmService.updateFilm(eq(FILM_ID), any(FilmRequest.class))).thenThrow(new ConflictException(message));

        mockMvc.perform(
                        put("/api/v1/films/{id}", FILM_ID)
                                .with(adminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON_MAPPER.writeValueAsString(filmRequestFull()))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.code").value(ErrorCode.CONFLICT.name()));

        verify(filmService).updateFilm(eq(FILM_ID), any(FilmRequest.class));
    }

    @Test
    void shouldThrowOnUpdateIfInvalidRequest() throws Exception {
        mockMvc.perform(
                        put("/api/v1/films/{id}", FILM_ID)
                                .with(adminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON_MAPPER.writeValueAsString(invalidFilmRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.name()))
                .andExpect(jsonPath("$.errorDetails[*].field",
                        containsInAnyOrder(
                                "title",
                                "releaseYear",
                                "countries",
                                "description",
                                "posterName",
                                "genres",
                                "actors",
                                "directors")));

        verify(filmService, never()).updateFilm(eq(FILM_ID), any(FilmRequest.class));
    }

    @Test
    void shouldDeleteFilm() throws Exception {

        mockMvc.perform(delete("/api/v1/films/{id}", FILM_ID).with(adminJwt()))
                .andExpect(status().isNoContent());

        verify(filmService).deleteFilm(FILM_ID);
    }

    @Test
    void shouldThrowOnDeleteIfFilmNotFound() throws Exception {
        String message = "Film with id " + FILM_ID + " not found";
        doThrow(new ResourceNotFoundException(message)).when(filmService).deleteFilm(FILM_ID);

        mockMvc.perform(delete("/api/v1/films/{id}", FILM_ID).with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.name()));

        verify(filmService).deleteFilm(FILM_ID);
    }
}
