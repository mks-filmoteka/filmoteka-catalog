package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.film.dto.DetailedFilmResponse;
import io.github.mksfilmoteka.catalog.film.dto.FilmRequest;
import io.github.mksfilmoteka.catalog.film.dto.FilmResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static io.github.mksfilmoteka.catalog.film.FilmTestData.*;
import static io.github.mksfilmoteka.catalog.util.TestUtil.testListOf;
import static org.assertj.core.api.Assertions.assertThat;

class FilmMapperTest {

    private final FilmMapper filmMapper = Mappers.getMapper(FilmMapper.class);

    @Test
    void shouldMapFilmRequestToFilm() {
        Film film = filmMapper.filmRequestToFilm(filmRequest());

        assertThat(film.getTitle()).isEqualTo(FILM_TITLE);
        assertThat(film.getReleaseYear()).isEqualTo(RELEASE_YEAR);
        assertThat(film.getCountries()).containsExactlyInAnyOrder(Country.UNITED_STATES, Country.ITALY);
        assertThat(film.getDescription()).isEqualTo(FILM_DESCRIPTION);
        assertThat(film.getPosterName()).isEqualTo(FILM_POSTER_NAME);
        assertThat(film.getGenres()).containsExactlyInAnyOrder(Genre.ADVENTURE, Genre.ACTION);
    }

    @Test
    void shouldMapFilmRequestToFilmWithDistinctEnums() {
        FilmRequest request = new FilmRequest(
                0L,
                FILM_TITLE,
                RELEASE_YEAR,
                testListOf(Country.UNITED_STATES, Country.UNITED_STATES, Country.ITALY),
                FILM_DESCRIPTION,
                FILM_POSTER_NAME,
                testListOf(Genre.ACTION, Genre.ACTION, Genre.ADVENTURE),
                null,
                null);

        Film film = filmMapper.filmRequestToFilm(request);

        assertThat(film.getCountries()).containsExactly(Country.UNITED_STATES, Country.ITALY);
        assertThat(film.getGenres()).containsExactly(Genre.ACTION, Genre.ADVENTURE);
    }

    @Test
    void shouldMapFilmToDetailedFilmResponse() {
        DetailedFilmResponse response = filmMapper.filmToDetailedFilmResponse(loadedFilm());

        assertThat(response).isEqualTo(detailedFilmResponse());
    }

    @Test
    void shouldMapFilmToFilmResponse() {
        FilmResponse response = filmMapper.filmToFilmResponse(loadedFilm());

        assertThat(response).isEqualTo(filmResponse());
    }

    @Test
    void shouldMapUpdateFilmRequestToFilm() {
        Film film = loadedFilm();

        filmMapper.updateFilmRequestToFilm(updateFilmRequest(), film);

        assertThat(film.getTitle()).isEqualTo("updated title");
        assertThat(film.getReleaseYear()).isEqualTo(1999);
        assertThat(film.getCountries()).containsExactlyInAnyOrder(Country.CANADA);
        assertThat(film.getDescription()).isEqualTo(FILM_DESCRIPTION);
        assertThat(film.getGenres()).containsExactlyInAnyOrder(Genre.COMEDY);
    }
}
