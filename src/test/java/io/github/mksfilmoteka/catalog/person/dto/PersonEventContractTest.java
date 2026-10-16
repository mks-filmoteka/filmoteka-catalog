package io.github.mksfilmoteka.catalog.person.dto;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Pins the JSON that catalog writes to Kafka. The media service keeps its own copies
 * of these events, so a change that breaks one of these tests must be made there too.
 */
@JsonTest
class PersonEventContractTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant OCCURRED_AT = Instant.parse("2026-01-02T03:04:05Z");

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void shouldWritePersonDeletedEventInAgreedFormat() throws Exception {
        PersonDeletedEvent event = new PersonDeletedEvent(EVENT_ID, 123L, "photo.jpg", OCCURRED_AT);

        JSONAssert.assertEquals("""
                {
                  "eventId": "11111111-1111-4111-8111-111111111111",
                  "personId": 123,
                  "photoName": "photo.jpg",
                  "occurredAt": "2026-01-02T03:04:05Z"
                }
                """, jsonMapper.writeValueAsString(event), true);
    }

    @Test
    void shouldWritePersonPhotoChangedEventInAgreedFormat() throws Exception {
        PersonPhotoChangedEvent event = new PersonPhotoChangedEvent(EVENT_ID, 123L, "old.jpg", null, OCCURRED_AT);

        JSONAssert.assertEquals("""
                {
                  "eventId": "11111111-1111-4111-8111-111111111111",
                  "personId": 123,
                  "oldPhotoName": "old.jpg",
                  "newPhotoName": null,
                  "occurredAt": "2026-01-02T03:04:05Z"
                }
                """, jsonMapper.writeValueAsString(event), true);
    }
}
