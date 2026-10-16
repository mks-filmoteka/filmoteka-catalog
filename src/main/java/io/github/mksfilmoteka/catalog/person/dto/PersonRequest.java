package io.github.mksfilmoteka.catalog.person.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Request for creating or updating a person")
public record PersonRequest(
        @Schema(description = "Person name", example = "Keanu Reeves")
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "Date of birth", example = "1964-09-02")
        @PastOrPresent
        LocalDate birthDate,

        @Schema(description = "Date of death", example = "2024-01-01")
        @PastOrPresent
        LocalDate deathDate,

        @Schema(description = "Place of birth", example = "Beirut, Lebanon")
        @Size(max = 255)
        String placeOfBirth,

        @Schema(description = "Photo file name", example = "00000000-0000-0000-0000-000000000000.jpg")
        @Size(max = 1000)
        @Pattern(regexp = "^[a-zA-Z0-9_-]+\\.(jpg|jpeg|png|webp)$",
                message = "Photo file name must be a valid image file name")
        String photoName
) {
}
