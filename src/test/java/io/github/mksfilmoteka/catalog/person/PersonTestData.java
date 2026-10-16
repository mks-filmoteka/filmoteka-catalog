package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.person.dto.DetailedPersonResponse;
import io.github.mksfilmoteka.catalog.person.dto.PersonRequest;
import io.github.mksfilmoteka.catalog.person.dto.PersonResponse;

import java.time.LocalDate;

import static io.github.mksfilmoteka.catalog.film.FilmTestData.filmResponse;
import static io.github.mksfilmoteka.catalog.util.TestUtil.testListOf;

public final class PersonTestData {

    public static final String ACTOR_NAME = "actor name";
    public static final String DIRECTOR_NAME = "director name";
    public static final long ACTOR_ID = 1L;
    public static final long DIRECTOR_ID = 2L;
    public static final LocalDate BIRTH_DATE = LocalDate.of(1930, 5, 31);
    public static final LocalDate DEATH_DATE = LocalDate.of(2020, 1, 1);
    public static final String PLACE_OF_BIRTH = "San Francisco, USA";
    public static final String PHOTO_NAME = "11111111-1111-1111-1111-111111111111.jpg";

    public static Person person(String name) {
        Person person = new Person();
        person.setName(name);
        return person;
    }

    public static Person loadedActor() {
        Person person = person(ACTOR_NAME);
        person.setId(ACTOR_ID);
        return person;
    }

    public static Person loadedDirector() {
        Person person = person(DIRECTOR_NAME);
        person.setId(DIRECTOR_ID);
        return person;
    }

    public static Person loadedPersonWithDetails() {
        Person person = loadedActor();
        person.setBirthDate(BIRTH_DATE);
        person.setDeathDate(DEATH_DATE);
        person.setPlaceOfBirth(PLACE_OF_BIRTH);
        person.setPhotoName(PHOTO_NAME);
        return person;
    }

    public static PersonRequest actorRequest() {
        return new PersonRequest(ACTOR_NAME, null, null, null, null);
    }

    public static PersonRequest directorRequest() {
        return new PersonRequest(DIRECTOR_NAME, null, null, null, null);
    }

    public static PersonRequest personRequest() {
        return new PersonRequest(ACTOR_NAME, BIRTH_DATE, DEATH_DATE, PLACE_OF_BIRTH, PHOTO_NAME);
    }

    public static PersonRequest personRequestWithDates(LocalDate birthDate, LocalDate deathDate) {
        return new PersonRequest(ACTOR_NAME, birthDate, deathDate, PLACE_OF_BIRTH, PHOTO_NAME);
    }

    public static PersonResponse actorResponse() {
        return new PersonResponse(ACTOR_ID, ACTOR_NAME);
    }

    public static PersonResponse directorResponse() {
        return new PersonResponse(DIRECTOR_ID, DIRECTOR_NAME);
    }

    public static DetailedPersonResponse detailedPersonResponse() {
        return new DetailedPersonResponse(
                ACTOR_ID,
                ACTOR_NAME,
                BIRTH_DATE,
                DEATH_DATE,
                PLACE_OF_BIRTH,
                PHOTO_NAME,
                testListOf(filmResponse()),
                testListOf()
        );
    }
}
