package io.github.mksfilmoteka.catalog.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public void publishNext() {
        Optional<OutboxEvent> pending = outboxEventRepository.findFirstByPublishedTsIsNullOrderByCreatedTsAscIdAsc();

        if (pending.isEmpty()) {
            return;
        }

        OutboxEvent event = pending.get();

        try {
            kafkaTemplate
                    .send(event.getTopic(), event.getMessageKey(), event.getPayload())
                    .get(3, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing outbox event " + event.getId(), ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Failed to publish outbox event " + event.getId(), ex);
        }

        event.markPublished(Instant.now());
    }
}
