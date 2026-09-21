package io.github.mksfilmoteka.catalog.outbox;

import io.github.mksfilmoteka.catalog.config.RepositoryTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "app.outbox.cleanup.enabled=true")
@Import({RepositoryTestConfig.class, OutboxCleanup.class})
@Testcontainers(disabledWithoutDocker = true)
class OutboxCleanupTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxCleanup outboxCleanup;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldDeleteOnlyEventsPublishedBeforeConfiguredRetention() {
        Instant now = Instant.now();
        Instant createdTs = now.minus(30, ChronoUnit.DAYS);
        OutboxEvent expired = event(createdTs, now.minus(4, ChronoUnit.DAYS));
        OutboxEvent recentlyPublished = event(createdTs, now.minus(2, ChronoUnit.DAYS));
        OutboxEvent pending = event(createdTs, null);
        outboxEventRepository.saveAllAndFlush(List.of(expired, recentlyPublished, pending));

        outboxCleanup.cleanupPublished();
        entityManager.clear();

        assertThat(outboxEventRepository.findAll()).extracting(OutboxEvent::getId)
                .containsExactlyInAnyOrder(recentlyPublished.getId(), pending.getId());
    }

    @Test
    void shouldKeepEventsPublishedExactlyAtCutoff() {
        Instant cutoff = Instant.parse("2026-09-01T00:00:00Z");
        Instant createdTs = cutoff.minus(1, ChronoUnit.DAYS);
        OutboxEvent expired = event(createdTs, cutoff.minusSeconds(1));
        OutboxEvent atCutoff = event(createdTs, cutoff);
        outboxEventRepository.saveAllAndFlush(List.of(expired, atCutoff));

        int deleted = outboxEventRepository.deletePublishedBefore(cutoff);
        entityManager.clear();

        assertThat(deleted).isEqualTo(1);
        assertThat(outboxEventRepository.findAll()).extracting(OutboxEvent::getId).containsExactly(atCutoff.getId());
    }

    private static OutboxEvent event(Instant createdTs, Instant publishedTs) {
        return new OutboxEvent(
                UUID.randomUUID(),
                "film-deleted-test",
                "1",
                "{\"filmId\":1}",
                createdTs,
                publishedTs
        );
    }
}
