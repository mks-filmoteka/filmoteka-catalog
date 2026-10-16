package io.github.mksfilmoteka.catalog.person.dto;

import io.github.mksfilmoteka.catalog.film.dto.FilmResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Person response with details and films")
public record DetailedPersonResponse(
        @Schema(description = "Person id", example = "1")
        Long id,

        @Schema(description = "Person name", example = "Keanu Reeves")
        String name,

        @Schema(description = "Date of birth", example = "1964-09-02")
        LocalDate birthDate,

        @Schema(description = "Date of death", example = "2024-01-01")
        LocalDate deathDate,

        @Schema(description = "Place of birth", example = "Beirut, Lebanon")
        String placeOfBirth,

        @Schema(description = "Photo file name", example = "00000000-0000-0000-0000-000000000000.jpg")
        String photoName,

        @ArraySchema(arraySchema = @Schema(description = "Films the person acted in"),
                schema = @Schema(implementation = FilmResponse.class))
        List<FilmResponse> actingFilms,

        @ArraySchema(arraySchema = @Schema(description = "Films the person directed"),
                schema = @Schema(implementation = FilmResponse.class))
        List<FilmResponse> directingFilms
) {
}
