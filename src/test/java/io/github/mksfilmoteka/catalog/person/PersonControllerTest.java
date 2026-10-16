package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.auth.KeycloakRealmRoleConverter;
import io.github.mksfilmoteka.catalog.auth.SecurityConfig;
import io.github.mksfilmoteka.catalog.common.exception.ConflictException;
import io.github.mksfilmoteka.catalog.common.exception.ErrorCode;
import io.github.mksfilmoteka.catalog.common.exception.ResourceNotFoundException;
import io.github.mksfilmoteka.catalog.person.dto.PersonRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static io.github.mksfilmoteka.catalog.person.PersonTestData.*;
import static io.github.mksfilmoteka.catalog.util.TestUtil.JSON_MAPPER;
import static io.github.mksfilmoteka.catalog.util.TestUtil.adminJwt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonController.class)
@Import({SecurityConfig.class, KeycloakRealmRoleConverter.class})
class PersonControllerTest {

    @MockitoBean
    private PersonService personService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldFindPersonById() throws Exception {
        when(personService.findById(ACTOR_ID)).thenReturn(detailedPersonResponse());

        mockMvc.perform(get("/api/v1/people/{id}", ACTOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTOR_ID))
                .andExpect(jsonPath("$.name").value(ACTOR_NAME))
                .andExpect(jsonPath("$.birthDate").value(BIRTH_DATE.toString()))
                .andExpect(jsonPath("$.deathDate").value(DEATH_DATE.toString()))
                .andExpect(jsonPath("$.placeOfBirth").value(PLACE_OF_BIRTH))
                .andExpect(jsonPath("$.photoName").value(PHOTO_NAME))
                .andExpect(jsonPath("$.actingFilms[0].id").isNumber())
                .andExpect(jsonPath("$.directingFilms").isEmpty());

        verify(personService).findById(ACTOR_ID);
    }

    @Test
    void shouldThrowIfPersonNotFound() throws Exception {
        String message = "Person with id " + ACTOR_ID + " not found";
        when(personService.findById(ACTOR_ID)).thenThrow(new ResourceNotFoundException(message));

        mockMvc.perform(get("/api/v1/people/{id}", ACTOR_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.name()));
    }

    @Test
    void shouldUpdatePerson() throws Exception {
        when(personService.updatePerson(eq(ACTOR_ID), any(PersonRequest.class))).thenReturn(detailedPersonResponse());

        putPerson(personRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTOR_ID))
                .andExpect(jsonPath("$.name").value(ACTOR_NAME));

        verify(personService).updatePerson(ACTOR_ID, personRequest());
    }

    @Test
    void shouldThrowOnUpdateIfConflict() throws Exception {
        String message = "Person with name " + ACTOR_NAME + " already exists";
        when(personService.updatePerson(eq(ACTOR_ID), any(PersonRequest.class)))
                .thenThrow(new ConflictException(message));

        putPerson(personRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.code").value(ErrorCode.CONFLICT.name()));
    }

    @Test
    void shouldThrowOnUpdateIfNameIsBlank() throws Exception {
        putPerson(new PersonRequest("", null, null, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.name()))
                .andExpect(jsonPath("$.errorDetails[0].field").value("name"));

        verifyNoInteractions(personService);
    }

    @Test
    void shouldThrowOnUpdateIfDateIsInTheFuture() throws Exception {
        putPerson(personRequestWithDates(LocalDate.now().plusDays(1), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetails[0].field").value("birthDate"));

        verifyNoInteractions(personService);
    }

    @Test
    void shouldThrowOnUpdateIfDeathDateIsBeforeBirthDate() throws Exception {
        putPerson(personRequestWithDates(BIRTH_DATE, BIRTH_DATE.minusDays(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetails[0].message").value("Death date must not be before birth date"));

        verifyNoInteractions(personService);
    }

    @Test
    void shouldThrowOnUpdateIfDateIsBefore1850() throws Exception {
        putPerson(personRequestWithDates(LocalDate.of(1849, 12, 31), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetails[0].message").value("Dates must not be before 1850"));

        verifyNoInteractions(personService);
    }

    @Test
    void shouldDeletePerson() throws Exception {
        mockMvc.perform(delete("/api/v1/people/{id}", ACTOR_ID).with(adminJwt()))
                .andExpect(status().isNoContent());

        verify(personService).deletePerson(ACTOR_ID);
    }

    @Test
    void shouldThrowOnDeleteIfPersonNotFound() throws Exception {
        String message = "Person with id " + ACTOR_ID + " not found";
        doThrow(new ResourceNotFoundException(message)).when(personService).deletePerson(ACTOR_ID);

        mockMvc.perform(delete("/api/v1/people/{id}", ACTOR_ID).with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.name()));
    }

    @Test
    void shouldNotFindOldActorEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/actors/{id}", ACTOR_ID))
                .andExpect(status().isNotFound());
    }

    private ResultActions putPerson(PersonRequest request) throws Exception {
        return mockMvc.perform(put("/api/v1/people/{id}", ACTOR_ID)
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_MAPPER.writeValueAsString(request)));
    }
}
