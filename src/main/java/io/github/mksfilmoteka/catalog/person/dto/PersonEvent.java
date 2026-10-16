package io.github.mksfilmoteka.catalog.person.dto;

import java.time.Instant;
import java.util.UUID;

public interface PersonEvent {

    UUID eventId();

    Long personId();

    Instant occurredAt();
}
