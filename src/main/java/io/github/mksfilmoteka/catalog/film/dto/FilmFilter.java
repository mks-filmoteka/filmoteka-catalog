package io.github.mksfilmoteka.catalog.film.dto;

import io.github.mksfilmoteka.catalog.film.Country;
import io.github.mksfilmoteka.catalog.film.Genre;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Film search criteria")
public record FilmFilter(
        @Schema(description = "Film title filter", example = "Matrix")
        @Size(max = 255)
        String title,

        @Schema(description = "Release year filter lower range", example = "1900")
        @Min(1888)
        @Max(2100)
        Integer yearFrom,

        @Schema(description = "Release year filter upper range", example = "2050")
        @Min(1888)
        @Max(2100)
        Integer yearTo,

        @Schema(description = "Genres filter", example = "[\"ACTION\"]")
        Set<Genre> genres,

        @Schema(description = "Country filter", example = "[\"UNITED_STATES\"]")
        Set<Country> countries,

        @Schema(description = "Film id filter for collection requests", example = "[1, 2, 3]")
        Set<Long> ids
) {
}
