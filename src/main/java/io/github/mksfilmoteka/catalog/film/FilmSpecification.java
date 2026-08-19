package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.film.dto.FilmFilter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilmSpecification {

    public static Specification<Film> withFilters(FilmFilter filter) {
        return Specification.where(hasIds(filter.ids()))
                .and(hasTitle(filter.title()))
                .and(hasCountries(filter.countries()))
                .and(hasReleaseYearFrom(filter.yearFrom()))
                .and(hasReleaseYearTo(filter.yearTo()))
                .and(hasGenres(filter.genres()));
    }

    public static Specification<Film> withCollectionFilters(FilmFilter filter) {
        return withFilters(filter);
    }

    public static Specification<Film> hasIds(Set<Long> ids) {

        return (root, _, cb) -> {
            if (ids == null) {
                return cb.conjunction();
            }
            return ids.isEmpty()
                    ? cb.disjunction()
                    : root.get("id").in(ids);
        };
    }

    public static Specification<Film> hasTitle(String title) {

        return (root, _, cb) ->
                title == null || title.isBlank()
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Film> hasCountries(Set<Country> countries) {

        return (root, query, cb) -> {
            query.distinct(true);
            return countries == null || countries.isEmpty()
                    ? cb.conjunction()
                    : root.join("countries").in(countries);
        };
    }

    public static Specification<Film> hasReleaseYearFrom(Integer releaseYearFrom) {

        return (root, _, cb) ->
                releaseYearFrom == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("releaseYear"), releaseYearFrom);
    }

    public static Specification<Film> hasReleaseYearTo(Integer releaseYearTo) {

        return (root, _, cb) ->
                releaseYearTo == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("releaseYear"), releaseYearTo);
    }

    public static Specification<Film> hasGenres(Set<Genre> genres) {

        return (root, query, cb) -> {
            query.distinct(true);
            return genres == null || genres.isEmpty()
                    ? cb.conjunction()
                    : root.join("genres").in(genres);
        };
    }
}
