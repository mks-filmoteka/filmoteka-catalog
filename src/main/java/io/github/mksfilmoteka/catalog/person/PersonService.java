package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.person.dto.PersonRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;

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
}
