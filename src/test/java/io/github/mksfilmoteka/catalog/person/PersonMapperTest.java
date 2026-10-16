package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.film.FilmMapper;
import io.github.mksfilmoteka.catalog.person.dto.DetailedPersonResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static io.github.mksfilmoteka.catalog.film.FilmTestData.loadedFilm;
import static io.github.mksfilmoteka.catalog.person.PersonTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

class PersonMapperTest {

    private final PersonMapper personMapper = Mappers.getMapper(PersonMapper.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(personMapper, "filmMapper", Mappers.getMapper(FilmMapper.class));
    }

    @Test
    void shouldMapPersonRequestToPerson() {
        Person person = personMapper.personRequestToPerson(personRequest());

        assertThat(person.getName()).isEqualTo(ACTOR_NAME);
        assertThat(person.getBirthDate()).isEqualTo(BIRTH_DATE);
        assertThat(person.getDeathDate()).isEqualTo(DEATH_DATE);
        assertThat(person.getPlaceOfBirth()).isEqualTo(PLACE_OF_BIRTH);
        assertThat(person.getPhotoName()).isEqualTo(PHOTO_NAME);
    }

    @Test
    void shouldMapPersonToDetailedPersonResponse() {
        Person person = loadedPersonWithDetails();
        person.getActingFilms().add(loadedFilm());

        DetailedPersonResponse response = personMapper.personToDetailedPersonResponse(person);

        assertThat(response).isEqualTo(detailedPersonResponse());
    }

    @Test
    void shouldReplaceAllDetailsOnUpdate() {
        Person person = loadedPersonWithDetails();

        personMapper.updatePersonRequestToPerson(directorRequest(), person);

        assertThat(person.getId()).isEqualTo(ACTOR_ID);
        assertThat(person.getName()).isEqualTo(DIRECTOR_NAME);
        assertThat(person.getBirthDate()).isNull();
        assertThat(person.getDeathDate()).isNull();
        assertThat(person.getPlaceOfBirth()).isNull();
        assertThat(person.getPhotoName()).isNull();
    }
}
