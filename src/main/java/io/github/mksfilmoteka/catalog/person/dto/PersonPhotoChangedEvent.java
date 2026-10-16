package io.github.mksfilmoteka.catalog.person.dto;

import java.time.Instant;
import java.util.UUID;

public record PersonPhotoChangedEvent(
        UUID eventId,
        Long personId,
        String oldPhotoName,
        String newPhotoName,
        Instant occurredAt
) implements PersonEvent {
}
