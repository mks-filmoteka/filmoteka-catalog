package io.github.mksfilmoteka.catalog.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Test
    void shouldDoNothingWhenNoEventsArePending() {
        when(outboxEventRepository.findFirstByPublishedTsIsNullOrderByCreatedTsAscIdAsc())
                .thenReturn(Optional.empty());

        outboxPublisher.publishNext();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldPublishEventAndMarkItPublished() {
        OutboxEvent event = pendingEvent();
        when(outboxEventRepository.findFirstByPublishedTsIsNullOrderByCreatedTsAscIdAsc())
                .thenReturn(Optional.of(event));
        when(kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(null));
        Instant beforePublishing = Instant.now();

        outboxPublisher.publishNext();

        Instant afterPublishing = Instant.now();
        verify(kafkaTemplate).send(event.getTopic(), event.getMessageKey(), event.getPayload());
        assertThat(event.getPublishedTs()).isBetween(beforePublishing, afterPublishing);
    }

    @Test
    void shouldLeaveEventPendingWhenPublishingFails() {
        OutboxEvent event = pendingEvent();
        var failure = new IllegalStateException("Kafka is unavailable");
        when(outboxEventRepository.findFirstByPublishedTsIsNullOrderByCreatedTsAscIdAsc())
                .thenReturn(Optional.of(event));
        when(kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload()))
                .thenReturn(CompletableFuture.failedFuture(failure));

        IllegalStateException exception = assertThrows(IllegalStateException.class, outboxPublisher::publishNext);

        assertThat(exception).hasRootCause(failure);
        assertThat(event.getPublishedTs()).isNull();
    }

    private static OutboxEvent pendingEvent() {
        return new OutboxEvent(UUID.randomUUID(), "film-deleted-test", "1", "{\"filmId\":1}", Instant.now(), null);
    }
}
