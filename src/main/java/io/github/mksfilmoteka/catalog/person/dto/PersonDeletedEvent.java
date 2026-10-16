package io.github.mksfilmoteka.catalog.person.dto;

import java.time.Instant;
import java.util.UUID;

public record PersonDeletedEvent(
        UUID eventId,
        Long personId,
        String photoName,
        Instant occurredAt
) implements PersonEvent {
}
