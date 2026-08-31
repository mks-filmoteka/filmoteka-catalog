package io.github.mksfilmoteka.catalog.film;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface FilmRepository extends JpaRepository<Film, Long>, JpaSpecificationExecutor<Film> {
    boolean existsByTitleAndReleaseYear(String title, int releaseYear);

    @Query("select film.id from Film film where film.id in :filmIds")
    Set<Long> findExistingFilmIds(@Param("filmIds") Set<Long> filmIds);
}
