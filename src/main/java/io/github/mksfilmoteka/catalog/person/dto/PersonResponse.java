package io.github.mksfilmoteka.catalog.person.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Person response")
public record PersonResponse(
        @Schema(description = "Person id", example = "1")
        Long id,

        @Schema(description = "Person name", example = "Keanu Reeves")
        String name
) {
}
