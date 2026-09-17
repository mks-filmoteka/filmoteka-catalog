package io.github.mksfilmoteka.catalog.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "outbox_event", schema = "filmoteka")
public class OutboxEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String topic;

    @Column(name = "message_key", nullable = false, updatable = false)
    private String messageKey;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_ts", nullable = false, updatable = false)
    private Instant createdTs;

    @Column(name = "published_ts")
    private Instant publishedTs;

    public void markPublished(Instant publishedTs) {
        this.publishedTs = publishedTs;
    }
}
