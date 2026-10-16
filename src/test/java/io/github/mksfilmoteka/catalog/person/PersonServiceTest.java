package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.common.exception.ConflictException;
import io.github.mksfilmoteka.catalog.common.exception.ResourceNotFoundException;
import io.github.mksfilmoteka.catalog.film.Film;
import io.github.mksfilmoteka.catalog.person.dto.DetailedPersonResponse;
import io.github.mksfilmoteka.catalog.person.dto.PersonRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static io.github.mksfilmoteka.catalog.film.FilmTestData.loadedFilm;
import static io.github.mksfilmoteka.catalog.person.PersonTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PersonMapper personMapper;

    @InjectMocks
    private PersonService personService;

    @Test
    void shouldNotCreatePersonIfFound() {
        Person loadedActor = loadedActor();
        when(personRepository.findByName(ACTOR_NAME)).thenReturn(Optional.of(loadedActor));

        Person person = personService.findOrCreate(personRequest());

        assertThat(person).isEqualTo(loadedActor);
        verify(personRepository, never()).save(any());
    }

    @Test
    void shouldCreatePersonIfNotFound() {
        Person person = person(ACTOR_NAME);
        when(personRepository.findByName(ACTOR_NAME)).thenReturn(Optional.empty());
        when(personMapper.personRequestToPerson(personRequest())).thenReturn(person);
        when(personRepository.save(person)).thenReturn(loadedActor());

        Person saved = personService.findOrCreate(personRequest());

        assertThat(saved.getName()).isEqualTo(ACTOR_NAME);
        verify(personRepository).save(person);
    }

    @Test
    void shouldFindPersonByIdIfExists() {
        Person person = loadedPersonWithDetails();
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.of(person));
        when(personMapper.personToDetailedPersonResponse(person)).thenReturn(detailedPersonResponse());

        DetailedPersonResponse response = personService.findById(ACTOR_ID);

        assertThat(response).isEqualTo(detailedPersonResponse());
    }

    @Test
    void shouldThrowIfPersonDoesNotExist() {
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> personService.findById(ACTOR_ID));

        verifyNoInteractions(personMapper);
    }

    @Test
    void shouldUpdatePersonIfExists() {
        Person person = loadedActor();
        PersonRequest request = personRequest();
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.of(person));
        when(personRepository.save(person)).thenReturn(person);
        when(personMapper.personToDetailedPersonResponse(person)).thenReturn(detailedPersonResponse());

        DetailedPersonResponse response = personService.updatePerson(ACTOR_ID, request);

        assertThat(response).isEqualTo(detailedPersonResponse());
        verify(personRepository, never()).existsByName(any());
        verify(personMapper).updatePersonRequestToPerson(request, person);
        verify(personRepository).save(person);
    }

    @Test
    void shouldThrowOnUpdateIfNameAlreadyExists() {
        Person person = loadedActor();
        person.setName("old name");
        PersonRequest request = personRequest();
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.of(person));
        when(personRepository.existsByName(ACTOR_NAME)).thenReturn(true);

        assertThrows(ConflictException.class, () -> personService.updatePerson(ACTOR_ID, request));

        verify(personRepository, never()).save(any());
        verifyNoInteractions(personMapper);
    }

    @Test
    void shouldThrowOnUpdateIfPersonDoesNotExist() {
        PersonRequest request = personRequest();
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> personService.updatePerson(ACTOR_ID, request));

        verify(personRepository, never()).save(any());
    }

    @Test
    void shouldRemovePersonFromFilmsBeforeDeleting() {
        Film acted = loadedFilm();
        Film directed = loadedFilm();
        Person person = loadedActor();
        acted.addActor(person);
        directed.addDirector(person);
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.of(person));

        personService.deletePerson(ACTOR_ID);

        assertThat(acted.getActors()).doesNotContain(person);
        assertThat(directed.getDirectors()).doesNotContain(person);
        assertThat(person.getActingFilms()).isEmpty();
        assertThat(person.getDirectingFilms()).isEmpty();
        verify(personRepository).delete(person);
    }

    @Test
    void shouldThrowOnDeleteIfPersonDoesNotExist() {
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> personService.deletePerson(ACTOR_ID));

        verify(personRepository, never()).delete(any());
    }
}
