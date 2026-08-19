package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.common.PageResponse;
import io.github.mksfilmoteka.catalog.common.exception.BadRequestException;
import io.github.mksfilmoteka.catalog.common.exception.ErrorResponse;
import io.github.mksfilmoteka.catalog.film.dto.DetailedFilmResponse;
import io.github.mksfilmoteka.catalog.film.dto.FilmFilter;
import io.github.mksfilmoteka.catalog.film.dto.FilmRequest;
import io.github.mksfilmoteka.catalog.film.dto.FilmResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Films", description = "Operations related to films")
@RestController
@RequestMapping("/api/v1/films")
@RequiredArgsConstructor
public class FilmController {

    private final FilmService filmService;

    @Operation(
            summary = "Get list of films",
            description = "Returns page of films, filtered and sorted"
    )
    @ApiResponse(responseCode = "200", description = "Page returned",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PageResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Bad request",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @GetMapping
    public ResponseEntity<PageResponse<FilmResponse>> getFilms(
            @ParameterObject @Valid FilmFilter filter,
            @ParameterObject Pageable pageable) {
        PageResponse<FilmResponse> response = filmService.getFilms(filter, fixedPageable(pageable));

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get film collection",
            description = "Returns page of films limited to the given film ids, filtered and sorted"
    )
    @ApiResponse(responseCode = "200", description = "Page returned",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PageResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Bad request",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @PostMapping("/collection")
    public ResponseEntity<PageResponse<FilmResponse>> getFilmCollection(
            @RequestBody @Valid FilmFilter filter,
            @ParameterObject Pageable pageable) {
        if (filter.ids() == null) {
            throw new BadRequestException("Film ids are required for collection search");
        }
        PageResponse<FilmResponse> response = filmService.getFilmCollection(filter, fixedPageable(pageable));

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get film by id",
            description = "Returns film with details including list of actors and directors"
    )
    @ApiResponse(responseCode = "200", description = "Film returned",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DetailedFilmResponse.class)
            )
    )
    @ApiResponse(responseCode = "404", description = "Film not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @GetMapping("/{id}")
    public ResponseEntity<DetailedFilmResponse> getFilm(@PathVariable Long id) {
        DetailedFilmResponse response = filmService.findById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create new film",
            description = "Creates a film with actors, directors and genres"
    )
    @ApiResponse(responseCode = "201", description = "Film created",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DetailedFilmResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(responseCode = "409", description = "Film already exists",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @PostMapping
    public ResponseEntity<DetailedFilmResponse> createFilm(@RequestBody @Valid FilmRequest request) {
        DetailedFilmResponse response = filmService.createFilm(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update film",
            description = "Update film fields"
    )
    @ApiResponse(responseCode = "200", description = "Film updated",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DetailedFilmResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(responseCode = "404", description = "Film not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(responseCode = "409", description = "Title and releaseYear conflict",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @PutMapping("/{id}")
    public ResponseEntity<DetailedFilmResponse> updateFilm(
            @PathVariable Long id,
            @RequestBody @Valid FilmRequest request) {
        DetailedFilmResponse response = filmService.updateFilm(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete film",
            description = "Deletes film by id"
    )
    @ApiResponse(responseCode = "204", description = "Film deleted")
    @ApiResponse(responseCode = "404", description = "Film not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFilm(@PathVariable Long id) {
        filmService.deleteFilm(id);
        return ResponseEntity.noContent().build();
    }

    private static Pageable fixedPageable(Pageable pageable) {
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        int pageSize = 100;
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort().and(Sort.by("id").ascending())
                : Sort.by("id").ascending();

        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
