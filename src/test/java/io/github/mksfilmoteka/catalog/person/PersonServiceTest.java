package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.common.exception.ConflictException;
import io.github.mksfilmoteka.catalog.common.exception.ResourceNotFoundException;
import io.github.mksfilmoteka.catalog.film.Film;
import io.github.mksfilmoteka.catalog.outbox.OutboxEvent;
import io.github.mksfilmoteka.catalog.outbox.OutboxEventRepository;
import io.github.mksfilmoteka.catalog.person.dto.DetailedPersonResponse;
import io.github.mksfilmoteka.catalog.person.dto.PersonDeletedEvent;
import io.github.mksfilmoteka.catalog.person.dto.PersonPhotoChangedEvent;
import io.github.mksfilmoteka.catalog.person.dto.PersonRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static io.github.mksfilmoteka.catalog.film.FilmTestData.loadedFilm;
import static io.github.mksfilmoteka.catalog.person.PersonTestData.*;
import static io.github.mksfilmoteka.catalog.util.TestUtil.JSON_MAPPER;
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

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private PersonService personService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(personService, "personPhotoChangedTopic", PERSON_PHOTO_CHANGED_TOPIC);
        ReflectionTestUtils.setField(personService, "personDeletedTopic", PERSON_DELETED_TOPIC);
    }

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
        verifyNoInteractions(outboxEventRepository, jsonMapper);
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
        verifyNoInteractions(outboxEventRepository, jsonMapper);
    }

    @Test
    void shouldThrowOnDeleteIfPersonDoesNotExist() {
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> personService.deletePerson(ACTOR_ID));

        verify(personRepository, never()).delete(any());
    }

    @Test
    void shouldPublishPhotoChangedWhenPhotoIsReplaced() {
        OutboxEvent outboxEvent = replacePhoto(personRequest());

        assertThat(outboxEvent.getTopic()).isEqualTo(PERSON_PHOTO_CHANGED_TOPIC);
        assertThat(outboxEvent.getMessageKey()).isEqualTo(Long.toString(ACTOR_ID));
        PersonPhotoChangedEvent event = JSON_MAPPER.readValue(outboxEvent.getPayload(), PersonPhotoChangedEvent.class);
        assertThat(event.eventId()).isEqualTo(outboxEvent.getId());
        assertThat(event.personId()).isEqualTo(ACTOR_ID);
        assertThat(event.oldPhotoName()).isEqualTo("old.jpg");
        assertThat(event.newPhotoName()).isEqualTo(PHOTO_NAME);
    }

    @Test
    void shouldPublishPhotoChangedWhenPhotoIsRemoved() {
        OutboxEvent outboxEvent = replacePhoto(actorRequest());

        PersonPhotoChangedEvent event = JSON_MAPPER.readValue(outboxEvent.getPayload(), PersonPhotoChangedEvent.class);
        assertThat(event.oldPhotoName()).isEqualTo("old.jpg");
        assertThat(event.newPhotoName()).isNull();
    }

    @Test
    void shouldNotPublishWhenPhotoIsUnchanged() {
        Person person = loadedPersonWithDetails();
        PersonRequest request = personRequest();
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.of(person));
        doAnswer(updatePhotoOnly()).when(personMapper).updatePersonRequestToPerson(request, person);
        when(personRepository.save(person)).thenReturn(person);

        personService.updatePerson(ACTOR_ID, request);

        verifyNoInteractions(outboxEventRepository, jsonMapper);
    }

    @Test
    void shouldPublishPersonDeletedWhenPersonHadPhoto() {
        Person person = loadedPersonWithDetails();
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.of(person));

        personService.deletePerson(ACTOR_ID);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent outboxEvent = captor.getValue();
        assertThat(outboxEvent.getTopic()).isEqualTo(PERSON_DELETED_TOPIC);
        assertThat(outboxEvent.getMessageKey()).isEqualTo(Long.toString(ACTOR_ID));
        PersonDeletedEvent event = JSON_MAPPER.readValue(outboxEvent.getPayload(), PersonDeletedEvent.class);
        assertThat(event.eventId()).isEqualTo(outboxEvent.getId());
        assertThat(event.personId()).isEqualTo(ACTOR_ID);
        assertThat(event.photoName()).isEqualTo(PHOTO_NAME);
        verify(personRepository).delete(person);
    }

    private OutboxEvent replacePhoto(PersonRequest request) {
        Person person = loadedActor();
        person.setPhotoName("old.jpg");
        when(personRepository.findById(ACTOR_ID)).thenReturn(Optional.of(person));
        doAnswer(updatePhotoOnly()).when(personMapper).updatePersonRequestToPerson(request, person);
        when(personRepository.save(person)).thenReturn(person);

        personService.updatePerson(ACTOR_ID, request);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Answer<Void> updatePhotoOnly() {
        return invocation -> {
            PersonRequest request = invocation.getArgument(0);
            Person person = invocation.getArgument(1);
            person.setPhotoName(request.photoName());
            return null;
        };
    }
}