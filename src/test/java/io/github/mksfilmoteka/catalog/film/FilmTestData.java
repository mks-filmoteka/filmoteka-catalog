package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.film.dto.DetailedFilmResponse;
import io.github.mksfilmoteka.catalog.film.dto.FilmFilter;
import io.github.mksfilmoteka.catalog.film.dto.FilmRequest;
import io.github.mksfilmoteka.catalog.film.dto.FilmResponse;

import static io.github.mksfilmoteka.catalog.actor.ActorTestData.actorRequest;
import static io.github.mksfilmoteka.catalog.actor.ActorTestData.actorResponse;
import static io.github.mksfilmoteka.catalog.director.DirectorTestData.directorRequest;
import static io.github.mksfilmoteka.catalog.director.DirectorTestData.directorResponse;
import static io.github.mksfilmoteka.catalog.util.TestUtil.testListOf;
import static io.github.mksfilmoteka.catalog.util.TestUtil.testSetOf;

public final class FilmTestData {

    public static final long FILM_ID = 1L;
    public static final String FILM_TITLE = "film title";
    public static final int RELEASE_YEAR = 2000;
    public static final String FILM_DESCRIPTION = "film description";
    public static final String FILM_POSTER_NAME = "00000000-0000-0000-0000-000000000000.jpg";

    public static Film film() {
        Film film = new Film();
        film.setTitle(FILM_TITLE);
        film.setReleaseYear(RELEASE_YEAR);
        film.setCountries(testListOf(Country.UNITED_STATES, Country.ITALY));
        film.setDescription(FILM_DESCRIPTION);
        film.setPosterName(FILM_POSTER_NAME);
        film.setGenres(testListOf(Genre.ADVENTURE, Genre.ACTION));
        return film;
    }

    public static Film loadedFilm() {
        Film film = film();
        film.setId(FILM_ID);
        return film;
    }

    public static FilmRequest filmRequest() {
        return new FilmRequest(
                FILM_TITLE,
                RELEASE_YEAR,
                testListOf(Country.UNITED_STATES, Country.ITALY),
                FILM_DESCRIPTION,
                FILM_POSTER_NAME,
                testListOf(Genre.ADVENTURE, Genre.ACTION),
                null,
                null
        );
    }

    public static FilmRequest invalidFilmRequest() {
        return new FilmRequest(
                "",
                1700,
                testListOf(),
                "",
                "",
                testListOf(),
                testListOf(),
                testListOf()
        );
    }

    public static FilmRequest filmRequestFull() {
        return new FilmRequest(
                FILM_TITLE,
                RELEASE_YEAR,
                testListOf(Country.UNITED_STATES, Country.ITALY),
                FILM_DESCRIPTION,
                FILM_POSTER_NAME,
                testListOf(Genre.ADVENTURE, Genre.ACTION),
                testListOf(actorRequest()),
                testListOf(directorRequest())
        );
    }

    public static FilmRequest filmRequestWithDuplications() {
        return new FilmRequest(
                FILM_TITLE,
                RELEASE_YEAR,
                testListOf(Country.UNITED_STATES, Country.ITALY),
                FILM_DESCRIPTION,
                FILM_POSTER_NAME,
                testListOf(Genre.ADVENTURE, Genre.ACTION),
                testListOf(actorRequest(), actorRequest()),
                testListOf(directorRequest(), directorRequest())
        );
    }

    public static FilmRequest updateFilmRequest() {
        return new FilmRequest(
                "updated title",
                1999,
                testListOf(Country.CANADA),
                FILM_DESCRIPTION,
                FILM_POSTER_NAME,
                testListOf(Genre.COMEDY),
                null,
                null
        );
    }

    public static DetailedFilmResponse detailedFilmResponse() {
        return new DetailedFilmResponse(
                FILM_ID,
                FILM_TITLE,
                RELEASE_YEAR,
                testListOf(Country.UNITED_STATES, Country.ITALY),
                FILM_DESCRIPTION,
                FILM_POSTER_NAME,
                testListOf(Genre.ADVENTURE, Genre.ACTION),
                testListOf(),
                testListOf()
        );
    }

    public static DetailedFilmResponse detailedFilmResponseFull() {
        return new DetailedFilmResponse(
                FILM_ID,
                FILM_TITLE,
                RELEASE_YEAR,
                testListOf(Country.UNITED_STATES, Country.ITALY),
                FILM_DESCRIPTION,
                FILM_POSTER_NAME,
                testListOf(Genre.ADVENTURE, Genre.ACTION),
                testListOf(actorResponse()),
                testListOf(directorResponse())
        );
    }

    public static FilmResponse filmResponse() {
        return new FilmResponse(
                FILM_ID,
                FILM_TITLE,
                testListOf(Country.UNITED_STATES, Country.ITALY),
                RELEASE_YEAR,
                FILM_POSTER_NAME,
                testListOf(Genre.ADVENTURE, Genre.ACTION)
        );
    }

    public static FilmFilter filmFilter() {
        return new FilmFilter(
                FILM_TITLE,
                RELEASE_YEAR,
                RELEASE_YEAR + 10,
                testSetOf(Genre.ADVENTURE, Genre.ACTION),
                testSetOf(Country.UNITED_STATES, Country.ITALY),
                null);
    }

    public static FilmFilter emptyFilmFilter() {
        return new FilmFilter(null, null, null, null, null, null);
    }

    public static FilmFilter filmCollectionFilter() {
        return new FilmFilter(
                FILM_TITLE,
                RELEASE_YEAR,
                RELEASE_YEAR + 10,
                testSetOf(Genre.ADVENTURE, Genre.ACTION),
                testSetOf(Country.UNITED_STATES, Country.ITALY),
                testSetOf(FILM_ID)
        );
    }

    public static FilmFilter emptyFilmCollectionFilter() {
        return new FilmFilter(null, null, null, null, null, testSetOf());
    }

    public static String filmRequestJson(String countries, String genres) {
        return """
                {
                  "title": "film title",
                  "releaseYear": 2000,
                  "countries": %s,
                  "description": "film description",
                  "posterName": "00000000-0000-0000-0000-000000000000.jpg",
                  "genres": %s,
                  "actors": [{"name": "actor name"}],
                  "directors": [{"name": "director name"}]
                }
                """.formatted(countries, genres);
    }
}
