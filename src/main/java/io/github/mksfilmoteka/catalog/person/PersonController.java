package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.common.exception.ErrorResponse;
import io.github.mksfilmoteka.catalog.person.dto.DetailedPersonResponse;
import io.github.mksfilmoteka.catalog.person.dto.PersonRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "People", description = "Operations related to people")
@RestController
@RequestMapping("/api/v1/people")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @Operation(
            summary = "Get person by id",
            description = "Returns person with details and the films they acted in or directed"
    )
    @ApiResponse(responseCode = "200", description = "Person returned",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DetailedPersonResponse.class)
            )
    )
    @ApiResponse(responseCode = "404", description = "Person not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @GetMapping("/{id}")
    public ResponseEntity<DetailedPersonResponse> getPerson(@PathVariable Long id) {
        DetailedPersonResponse response = personService.findById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update person",
            description = "Update person details"
    )
    @ApiResponse(responseCode = "200", description = "Person updated",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DetailedPersonResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(responseCode = "404", description = "Person not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(responseCode = "409", description = "Name conflict",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @PutMapping("/{id}")
    public ResponseEntity<DetailedPersonResponse> updatePerson(
            @PathVariable Long id,
            @RequestBody @Valid PersonRequest request) {
        DetailedPersonResponse response = personService.updatePerson(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete person",
            description = "Removes the person from their films and deletes them"
    )
    @ApiResponse(responseCode = "204", description = "Person deleted")
    @ApiResponse(responseCode = "404", description = "Person not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable Long id) {
        personService.deletePerson(id);
        return ResponseEntity.noContent().build();
    }
}
