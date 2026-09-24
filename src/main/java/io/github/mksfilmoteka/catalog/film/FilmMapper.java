package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.film.dto.DetailedFilmResponse;
import io.github.mksfilmoteka.catalog.film.dto.FilmRequest;
import io.github.mksfilmoteka.catalog.film.dto.FilmResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FilmMapper {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "actors", ignore = true)
    @Mapping(target = "directors", ignore = true)
    Film filmRequestToFilm(FilmRequest request);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "actors", ignore = true)
    @Mapping(target = "directors", ignore = true)
    void updateFilmRequestToFilm(FilmRequest request, @MappingTarget Film film);

    FilmResponse filmToFilmResponse(Film film);

    DetailedFilmResponse filmToDetailedFilmResponse(Film film);

    List<FilmResponse> filmsToFilmResponses(List<Film> films);

    @AfterMapping
    default void normalizeEnums(FilmRequest request, @MappingTarget Film film) {
        if (request.genres() != null) {
            film.setGenres(request.genres().stream().distinct().toList());
        }
        if (request.countries() != null) {
            film.setCountries(request.countries().stream().distinct().toList());
        }
    }
}
