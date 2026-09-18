package io.github.mksfilmoteka.catalog.film.dto;

import java.time.Instant;
import java.util.UUID;

public record FilmPosterChangedEvent(
        UUID eventId,
        Long filmId,
        String oldPosterName,
        String newPosterName,
        Instant occurredAt
) implements FilmEvent {
}
