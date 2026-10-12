package io.github.mksfilmoteka.catalog.film.dto;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Pins the JSON that catalog writes to Kafka. The user and media services keep their own copies
 * of these events, so a change that breaks one of these tests must be made in those services too.
 */
@JsonTest
class FilmEventContractTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant OCCURRED_AT = Instant.parse("2026-01-02T03:04:05Z");

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void shouldWriteFilmDeletedEventInAgreedFormat() throws Exception {
        FilmDeletedEvent event = new FilmDeletedEvent(EVENT_ID, 123L, "poster.jpg", OCCURRED_AT);

        JSONAssert.assertEquals("""
                {
                  "eventId": "11111111-1111-4111-8111-111111111111",
                  "filmId": 123,
                  "posterName": "poster.jpg",
                  "occurredAt": "2026-01-02T03:04:05Z"
                }
                """, jsonMapper.writeValueAsString(event), true);
    }

    @Test
    void shouldWriteFilmPosterChangedEventInAgreedFormat() throws Exception {
        FilmPosterChangedEvent event = new FilmPosterChangedEvent(EVENT_ID, 123L, null, "new.jpg", OCCURRED_AT);

        JSONAssert.assertEquals("""
                {
                  "eventId": "11111111-1111-4111-8111-111111111111",
                  "filmId": 123,
                  "oldPosterName": null,
                  "newPosterName": "new.jpg",
                  "occurredAt": "2026-01-02T03:04:05Z"
                }
                """, jsonMapper.writeValueAsString(event), true);
    }
}
