package io.github.mksfilmoteka.catalog.film.dto;

import io.github.mksfilmoteka.catalog.actor.dto.ActorRequest;
import io.github.mksfilmoteka.catalog.director.dto.DirectorRequest;
import io.github.mksfilmoteka.catalog.film.Country;
import io.github.mksfilmoteka.catalog.film.Genre;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "Request for creating or updating a film")
public record FilmRequest(
        @Schema(description = "Film revision", example = "0")
        @PositiveOrZero
        Long version,

        @Schema(description = "Film title", example = "Matrix")
        @NotBlank
        @Size(max = 255)
        String title,

        @Schema(description = "Film release year", example = "1999")
        @NotNull
        @Min(1888)
        @Max(2100)
        Integer releaseYear,

        @Schema(description = "Film countries of production", example = "[\"United States\", \"Italy\"]")
        @NotEmpty
        @Size(max = 5)
        List<@NotNull @Valid Country> countries,

        @Schema(description = "Film description", example = "film description")
        @NotBlank
        @Size(max = 1000)
        String description,

        @Schema(description = "Film poster name", example = "00000000-0000-0000-0000-000000000000.jpg")
        @Size(max = 1000)
        @Pattern(regexp = "^[a-zA-Z0-9_-]+\\.(jpg|jpeg|png|webp)$",
                message = "Poster file name must be a valid image file name")
        String posterName,

        @Schema(description = "Film genres", example = "[\"Action\", \"Adventure\"]")
        @NotEmpty
        @Size(max = 5)
        List<@NotNull @Valid Genre> genres,

        @ArraySchema(schema = @Schema(implementation = ActorRequest.class))
        @NotEmpty
        @Size(max = 20)
        List<@NotNull @Valid ActorRequest> actors,

        @ArraySchema(schema = @Schema(implementation = DirectorRequest.class))
        @NotEmpty
        @Size(max = 5)
        List<@NotNull @Valid DirectorRequest> directors
) {

}
