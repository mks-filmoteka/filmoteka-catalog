package io.github.mksfilmoteka.catalog.film.dto;

import java.time.Instant;
import java.util.UUID;

public interface FilmEvent {

    UUID eventId();

    Long filmId();

    Instant occurredAt();
}
