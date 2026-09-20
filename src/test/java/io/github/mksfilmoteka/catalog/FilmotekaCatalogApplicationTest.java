package io.github.mksfilmoteka.catalog;

import io.github.mksfilmoteka.catalog.auth.KeycloakRealmRoleConverter;
import io.github.mksfilmoteka.catalog.auth.SecurityConfig;
import io.github.mksfilmoteka.catalog.common.exception.ErrorCode;
import io.github.mksfilmoteka.catalog.config.RepositoryTestConfig;
import io.github.mksfilmoteka.catalog.film.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import static io.github.mksfilmoteka.catalog.actor.ActorTestData.ACTOR_NAME;
import static io.github.mksfilmoteka.catalog.director.DirectorTestData.DIRECTOR_NAME;
import static io.github.mksfilmoteka.catalog.film.FilmTestData.*;
import static io.github.mksfilmoteka.catalog.util.TestUtil.JSON_MAPPER;
import static io.github.mksfilmoteka.catalog.util.TestUtil.adminJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FilmotekaCatalogApplication.class)
@AutoConfigureMockMvc
@Import({RepositoryTestConfig.class, SecurityConfig.class, KeycloakRealmRoleConverter.class})
@Testcontainers(disabledWithoutDocker = true)
class FilmotekaCatalogApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateFilmAndReadItThroughApi() throws Exception {
        String filmRequest = JSON_MAPPER.writeValueAsString(filmRequestFull());

        String createResponse = mockMvc.perform(post("/api/v1/films")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value(FILM_TITLE))
                .andExpect(jsonPath("$.releaseYear").value(RELEASE_YEAR))
                .andExpect(jsonPath("$.countries[0]").value(Country.UNITED_STATES.getJsonValue()))
                .andExpect(jsonPath("$.actors[0].id").isNumber())
                .andExpect(jsonPath("$.actors[0].name").value(ACTOR_NAME))
                .andExpect(jsonPath("$.directors[0].id").isNumber())
                .andExpect(jsonPath("$.directors[0].name").value(DIRECTOR_NAME))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdFilm = JSON_MAPPER.readTree(createResponse);
        long filmId = createdFilm.get("id").asLong();
        long actorId = createdFilm.get("actors").get(0).get("id").asLong();
        long directorId = createdFilm.get("directors").get(0).get("id").asLong();

        mockMvc.perform(get("/api/v1/films/{id}", filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(filmId))
                .andExpect(jsonPath("$.title").value(FILM_TITLE))
                .andExpect(jsonPath("$.actors[0].id").value(actorId))
                .andExpect(jsonPath("$.directors[0].id").value(directorId));

        mockMvc.perform(get("/api/v1/actors/{id}", actorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(actorId))
                .andExpect(jsonPath("$.name").value(ACTOR_NAME))
                .andExpect(jsonPath("$.films[0].id").value(filmId))
                .andExpect(jsonPath("$.films[0].title").value(FILM_TITLE));

        mockMvc.perform(get("/api/v1/directors/{id}", directorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(directorId))
                .andExpect(jsonPath("$.name").value(DIRECTOR_NAME))
                .andExpect(jsonPath("$.films[0].id").value(filmId))
                .andExpect(jsonPath("$.films[0].title").value(FILM_TITLE));

        mockMvc.perform(get("/api/v1/films")
                        .param("title", FILM_TITLE)
                        .param("yearFrom", String.valueOf(RELEASE_YEAR))
                        .param("yearTo", String.valueOf(RELEASE_YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(filmId))
                .andExpect(jsonPath("$.content[0].title").value(FILM_TITLE));

        mockMvc.perform(post("/api/v1/films")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONFLICT.name()));
    }
}
