package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.person.dto.PersonRequest;
import io.github.mksfilmoteka.catalog.person.dto.PersonResponse;

public final class PersonTestData {

    public static final String ACTOR_NAME = "actor name";
    public static final String DIRECTOR_NAME = "director name";
    public static final long ACTOR_ID = 1L;
    public static final long DIRECTOR_ID = 2L;

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

    public static PersonRequest actorRequest() {
        return new PersonRequest(ACTOR_NAME, null, null, null, null);
    }

    public static PersonRequest directorRequest() {
        return new PersonRequest(DIRECTOR_NAME, null, null, null, null);
    }

    public static PersonResponse actorResponse() {
        return new PersonResponse(ACTOR_ID, ACTOR_NAME);
    }

    public static PersonResponse directorResponse() {
        return new PersonResponse(DIRECTOR_ID, DIRECTOR_NAME);
    }
}
