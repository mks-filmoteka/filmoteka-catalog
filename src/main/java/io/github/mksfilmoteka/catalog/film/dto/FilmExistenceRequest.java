package io.github.mksfilmoteka.catalog.film.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Film ids to check for existence")
public record FilmExistenceRequest(

        @Schema(description = "Film ids", example = "[1, 2, 3]")
        @NotEmpty
        @Size(max = 500)
        Set<@NotNull @Positive Long> filmIds

) {
}
