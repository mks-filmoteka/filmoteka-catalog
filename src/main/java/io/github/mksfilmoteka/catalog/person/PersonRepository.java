package io.github.mksfilmoteka.catalog.person;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByName(String name);

    boolean existsByName(String name);
}
