package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.FilmotekaCatalogApplication;
import io.github.mksfilmoteka.catalog.config.RepositoryTestConfig;
import io.github.mksfilmoteka.catalog.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.github.mksfilmoteka.catalog.film.FilmTestData.FILM_DELETED_TOPIC;
import static io.github.mksfilmoteka.catalog.film.FilmTestData.film;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(classes = FilmotekaCatalogApplication.class)
@Import(RepositoryTestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class FilmDeletionOutboxTest {

    @Autowired
    private FilmService filmService;

    @MockitoSpyBean
    private FilmRepository filmRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        filmRepository.deleteAll();
    }

    @Test
    void shouldCommitFilmDeletionAndOutboxEventTogether() {
        Film saved = filmRepository.saveAndFlush(film());

        filmService.deleteFilm(saved.getId());

        assertFalse(filmRepository.existsById(saved.getId()));
        assertThat(outboxEventRepository.findAll())
                .singleElement()
                .satisfies(event -> {
                    assertEquals(FILM_DELETED_TOPIC, event.getTopic());
                    assertEquals(saved.getId().toString(), event.getMessageKey());
                    assertNull(event.getPublishedTs());
                });
    }

    @Test
    void shouldRollBackOutboxEventWhenFilmDeletionFails() {
        Film saved = filmRepository.saveAndFlush(film());
        Long filmId = saved.getId();

        doAnswer(_ -> {
            outboxEventRepository.flush();
            throw new DataAccessResourceFailureException("Simulated film deletion failure");
        }).when(filmRepository).delete(any(Film.class));

        assertThrows(DataAccessResourceFailureException.class, () -> filmService.deleteFilm(filmId));
        assertTrue(filmRepository.existsById(saved.getId()));
        assertThat(outboxEventRepository.count()).isZero();
    }
}
