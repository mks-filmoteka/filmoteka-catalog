package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.config.RepositoryTestConfig;
import io.github.mksfilmoteka.catalog.film.dto.FilmFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static io.github.mksfilmoteka.catalog.film.FilmTestData.*;
import static io.github.mksfilmoteka.catalog.util.TestUtil.testListOf;
import static io.github.mksfilmoteka.catalog.util.TestUtil.testSetOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(RepositoryTestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class FilmRepositoryTest {

    @Autowired
    private FilmRepository filmRepository;

    @Test
    void shouldSaveAndLoadFilm() {
        Film savedFilm = filmRepository.saveAndFlush(film());
        Optional<Film> loadedFilm = filmRepository.findById(savedFilm.getId());

        assertNotNull(savedFilm.getId());
        assertTrue(loadedFilm.isPresent());
        assertEquals(FILM_TITLE, loadedFilm.get().getTitle());
        assertEquals(RELEASE_YEAR, loadedFilm.get().getReleaseYear());
        assertEquals(FILM_DESCRIPTION, loadedFilm.get().getDescription());
        assertEquals(FILM_POSTER_NAME, loadedFilm.get().getPosterName());
        assertThat(loadedFilm.get().getCountries()).containsExactlyInAnyOrder(Country.UNITED_STATES, Country.ITALY);
        assertThat(loadedFilm.get().getGenres()).containsExactlyInAnyOrder(Genre.ADVENTURE, Genre.ACTION);
    }

    @Test
    void shouldFindPagedFilmsByFilter() {
        Film nonComplFilm = film();
        nonComplFilm.setTitle("different title");
        nonComplFilm.setReleaseYear(1999);
        nonComplFilm.setCountries(testListOf(Country.CANADA));
        nonComplFilm.setGenres(testListOf(Genre.CRIME));
        filmRepository.saveAndFlush(nonComplFilm);
        filmRepository.saveAndFlush(film());

        Pageable pageable = PageRequest.of(0, 100);
        Page<Film> page = filmRepository.findAll(FilmSpecification.withFilters(filmFilter()), pageable);

        assertThat(page.getContent()).extracting(Film::getTitle).containsExactlyInAnyOrder(FILM_TITLE);
        assertThat(page.getContent()).extracting(Film::getReleaseYear).containsExactlyInAnyOrder(RELEASE_YEAR);
        assertThat(page.getContent().getFirst().getCountries()).containsExactlyInAnyOrder(Country.UNITED_STATES, Country.ITALY);
        assertThat(page.getContent().getFirst().getGenres()).containsExactlyInAnyOrder(Genre.ADVENTURE, Genre.ACTION);
    }

    @Test
    void shouldFindPagedFilmCollectionByIdsAndFilter() {
        Film matchingFilm = filmRepository.saveAndFlush(film());

        Film sameFilterOutsideCollection = film();
        sameFilterOutsideCollection.setTitle("film title outside collection");
        sameFilterOutsideCollection.setReleaseYear(2001);
        filmRepository.saveAndFlush(sameFilterOutsideCollection);

        Film inCollectionWrongFilter = film();
        inCollectionWrongFilter.setTitle("other title");
        inCollectionWrongFilter.setReleaseYear(2002);
        filmRepository.saveAndFlush(inCollectionWrongFilter);

        FilmFilter filter = new FilmFilter(
                FILM_TITLE,
                RELEASE_YEAR,
                RELEASE_YEAR + 10,
                testSetOf(Genre.ADVENTURE, Genre.ACTION),
                testSetOf(Country.UNITED_STATES, Country.ITALY),
                testSetOf(matchingFilm.getId(), inCollectionWrongFilter.getId())
        );

        Page<Film> page = filmRepository.findAll(
                FilmSpecification.withCollectionFilters(filter),
                PageRequest.of(0, 100));

        assertThat(page.getContent())
                .extracting(Film::getId)
                .containsExactly(matchingFilm.getId());
    }

    @Test
    void shouldReturnEmptyPageForEmptyFilmCollectionIds() {
        filmRepository.saveAndFlush(film());

        Page<Film> page = filmRepository.findAll(
                FilmSpecification.withCollectionFilters(emptyFilmCollectionFilter()),
                PageRequest.of(0, 100));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void shouldReturnFilmsSortedByTitleAscending() {
        Film filmA = film();
        filmA.setTitle("A");
        Film filmB = film();
        filmB.setTitle("B");
        Film filmC = film();
        filmC.setTitle("C");

        filmRepository.saveAndFlush(filmC);
        filmRepository.saveAndFlush(filmB);
        filmRepository.saveAndFlush(filmA);

        Page<Film> page = filmRepository.findAll(FilmSpecification.hasTitle(null),
                PageRequest.of(0, 100, Sort.by("title").ascending()));

        assertThat(page.getContent())
                .extracting(Film::getTitle)
                .containsExactly("A", "B", "C");
    }
}
