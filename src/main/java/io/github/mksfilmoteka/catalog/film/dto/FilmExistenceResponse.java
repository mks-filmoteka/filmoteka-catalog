package io.github.mksfilmoteka.catalog.film.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Result of checking film existence")
public record FilmExistenceResponse(

        @Schema(description = "Requested film ids that do not exist", example = "[2, 3]")
        Set<Long> missingFilmIds

) {
}
