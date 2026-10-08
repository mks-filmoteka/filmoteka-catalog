package io.github.mksfilmoteka.catalog.common.exception;

import io.github.mksfilmoteka.catalog.auth.KeycloakRealmRoleConverter;
import io.github.mksfilmoteka.catalog.auth.SecurityConfig;
import io.github.mksfilmoteka.catalog.film.FilmController;
import io.github.mksfilmoteka.catalog.film.FilmService;
import io.github.mksfilmoteka.catalog.film.dto.FilmRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static io.github.mksfilmoteka.catalog.film.FilmTestData.filmRequestFull;
import static io.github.mksfilmoteka.catalog.util.TestUtil.JSON_MAPPER;
import static io.github.mksfilmoteka.catalog.util.TestUtil.adminJwt;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilmController.class)
@Import({SecurityConfig.class, KeycloakRealmRoleConverter.class})
class GlobalExceptionHandlerTest {

    @MockitoBean
    private FilmService filmService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnNotFoundForUnknownPath() throws Exception {
        mockMvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/unknown"))
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.name()));
    }

    @Test
    void shouldReturnMethodNotAllowedForUnsupportedMethod() throws Exception {
        mockMvc.perform(patch("/api/v1/films/1").with(adminJwt()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.path").value("/api/v1/films/1"))
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.name()));
    }

    @Test
    void shouldReturnUnsupportedMediaTypeForWrongContentType() throws Exception {
        mockMvc.perform(post("/api/v1/films")
                        .with(adminJwt())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }

    @Test
    void shouldReturnBadRequestWithoutParserDetailsForMalformedBody() throws Exception {
        mockMvc.perform(post("/api/v1/films")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(jsonPath("$.message", not(containsString("JSON parse error"))))
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.name()));
    }

    @Test
    void shouldReturnConflictForDataIntegrityViolation() throws Exception {
        when(filmService.createFilm(any(FilmRequest.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates uk_film_title_year"));

        mockMvc.perform(post("/api/v1/films")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_MAPPER.writeValueAsString(filmRequestFull())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", not(containsString("uk_film_title_year"))))
                .andExpect(jsonPath("$.code").value(ErrorCode.CONFLICT.name()));
    }
}
