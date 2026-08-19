package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.actor.ActorService;
import io.github.mksfilmoteka.catalog.common.PageResponse;
import io.github.mksfilmoteka.catalog.common.exception.BadRequestException;
import io.github.mksfilmoteka.catalog.common.exception.ConflictException;
import io.github.mksfilmoteka.catalog.common.exception.ResourceNotFoundException;
import io.github.mksfilmoteka.catalog.director.DirectorService;
import io.github.mksfilmoteka.catalog.film.dto.DetailedFilmResponse;
import io.github.mksfilmoteka.catalog.film.dto.FilmFilter;
import io.github.mksfilmoteka.catalog.film.dto.FilmRequest;
import io.github.mksfilmoteka.catalog.film.dto.FilmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilmService {

    private final FilmRepository filmRepository;
    private final ActorService actorService;
    private final DirectorService directorService;
    private final FilmMapper filmMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "releaseYear", "id");

    public PageResponse<FilmResponse> getFilms(FilmFilter filter, Pageable pageable) {
        return searchFilms(filter, FilmSpecification.withFilters(filter), pageable);
    }

    public PageResponse<FilmResponse> getFilmCollection(FilmFilter filter, Pageable pageable) {
        if (filter.ids() == null) {
            throw new BadRequestException("Film ids are required for collection search");
        }
        return searchFilms(filter, FilmSpecification.withCollectionFilters(filter), pageable);
    }

    private PageResponse<FilmResponse> searchFilms(Object filter, Specification<Film> specification, Pageable pageable) {
        log.debug("Searching films. filter={}, pageable={}", filter, pageable);
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort field: " + order.getProperty());
            }
        });

        Page<Film> page = filmRepository.findAll(specification, pageable);
        List<FilmResponse> content = filmMapper.filmsToFilmResponses(page.getContent());

        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
    }

    public DetailedFilmResponse findById(Long id) {
        Film film = getFilmOrThrow(id);
        return filmMapper.filmToDetailedFilmResponse(film);
    }

    @Transactional
    public DetailedFilmResponse createFilm(FilmRequest request) {
        if (filmRepository.existsByTitleAndReleaseYear(request.title(), request.releaseYear())) {
            throw new ConflictException(String.format("Film with title '%s' and release year '%s' already exists",
                    request.title(), request.releaseYear()));
        }

        Film film = filmMapper.filmRequestToFilm(request);

        request.actors().stream()
                .distinct()
                .map(actorService::findOrCreate)
                .forEach(film::addActor);
        request.directors().stream()
                .distinct()
                .map(directorService::findOrCreate)
                .forEach(film::addDirector);

        Film saved = filmRepository.save(film);
        log.info("Created film id={}, title={}", saved.getId(), saved.getTitle());

        return filmMapper.filmToDetailedFilmResponse(saved);
    }

    @Transactional
    public DetailedFilmResponse updateFilm(Long id, FilmRequest request) {
        Film film = getFilmOrThrow(id);
        if ((!film.getTitle().equals(request.title()) || !film.getReleaseYear().equals(request.releaseYear()))
                && filmRepository.existsByTitleAndReleaseYear(request.title(), request.releaseYear())) {
            throw new ConflictException(String.format("Film with title '%s' and release year '%s' already exists",
                    request.title(), request.releaseYear()));
        }
        filmMapper.updateFilmRequestToFilm(request, film);

        film.getActors().clear();
        request.actors().stream()
                .distinct()
                .map(actorService::findOrCreate)
                .forEach(film::addActor);

        film.getDirectors().clear();
        request.directors().stream()
                .distinct()
                .map(directorService::findOrCreate)
                .forEach(film::addDirector);

        Film saved = filmRepository.save(film);
        log.info("Updated film id={} with title={}", saved.getId(), saved.getTitle());

        return filmMapper.filmToDetailedFilmResponse(saved);
    }

    @Transactional
    public void deleteFilm(Long id) {
        Film film = getFilmOrThrow(id);
        filmRepository.delete(film);
        log.info("Deleted film id={}", id);
    }

    private Film getFilmOrThrow(Long id) {
        return filmRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Film with id " + id + " not found"));
    }
}
