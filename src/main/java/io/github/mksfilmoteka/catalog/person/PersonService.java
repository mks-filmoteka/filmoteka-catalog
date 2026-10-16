package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.common.exception.ConflictException;
import io.github.mksfilmoteka.catalog.common.exception.ResourceNotFoundException;
import io.github.mksfilmoteka.catalog.film.Film;
import io.github.mksfilmoteka.catalog.outbox.OutboxEvent;
import io.github.mksfilmoteka.catalog.outbox.OutboxEventRepository;
import io.github.mksfilmoteka.catalog.person.dto.DetailedPersonResponse;
import io.github.mksfilmoteka.catalog.person.dto.PersonDeletedEvent;
import io.github.mksfilmoteka.catalog.person.dto.PersonEvent;
import io.github.mksfilmoteka.catalog.person.dto.PersonPhotoChangedEvent;
import io.github.mksfilmoteka.catalog.person.dto.PersonRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    @Value("${app.kafka.topics.person-photo-changed.name}")
    private String personPhotoChangedTopic;

    @Value("${app.kafka.topics.person-deleted.name}")
    private String personDeletedTopic;

    public DetailedPersonResponse findById(Long id) {
        Person person = getPersonOrThrow(id);
        return personMapper.personToDetailedPersonResponse(person);
    }

    @Transactional
    public Person findOrCreate(PersonRequest request) {
        Optional<Person> person = personRepository.findByName(request.name());
        if (person.isPresent()) {
            return person.get();
        }
        Person saved = personRepository.save(personMapper.personRequestToPerson(request));
        log.info("Created person id={}, name={}", saved.getId(), saved.getName());

        return saved;
    }

    @Transactional
    public DetailedPersonResponse updatePerson(Long id, PersonRequest request) {
        Person person = getPersonOrThrow(id);
        if (!person.getName().equals(request.name()) && personRepository.existsByName(request.name())) {
            throw new ConflictException("Person with name " + request.name() + " already exists");
        }
        String oldPhotoName = person.getPhotoName();
        personMapper.updatePersonRequestToPerson(request, person);

        Person saved = personRepository.save(person);

        if (oldPhotoName != null && !Objects.equals(oldPhotoName, saved.getPhotoName())) {
            saveOutboxEvent(personPhotoChangedTopic, new PersonPhotoChangedEvent(
                    UUID.randomUUID(),
                    saved.getId(),
                    oldPhotoName,
                    saved.getPhotoName(),
                    Instant.now()
            ));
        }
        log.info("Updated person id={} with name={}", saved.getId(), saved.getName());

        return personMapper.personToDetailedPersonResponse(saved);
    }

    @Transactional
    public void deletePerson(Long id) {
        Person person = getPersonOrThrow(id);
        for (Film film : List.copyOf(person.getActingFilms())) {
            film.removeActor(person);
        }
        for (Film film : List.copyOf(person.getDirectingFilms())) {
            film.removeDirector(person);
        }
        if (person.getPhotoName() != null) {
            saveOutboxEvent(personDeletedTopic, new PersonDeletedEvent(
                    UUID.randomUUID(),
                    person.getId(),
                    person.getPhotoName(),
                    Instant.now()
            ));
        }
        personRepository.delete(person);
        log.info("Deleted person id={}", id);
    }

    private void saveOutboxEvent(String topic, PersonEvent event) {
        OutboxEvent outboxEvent = new OutboxEvent(
                event.eventId(),
                topic,
                event.personId().toString(),
                jsonMapper.writeValueAsString(event),
                event.occurredAt(),
                null
        );
        outboxEventRepository.save(outboxEvent);
    }

    private Person getPersonOrThrow(Long id) {
        return personRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Person with id " + id + " not found"));
    }
}
