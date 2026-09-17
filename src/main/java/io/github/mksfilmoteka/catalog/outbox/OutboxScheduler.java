package io.github.mksfilmoteka.catalog.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxPublisher outboxPublisher;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void publishPending() {
        try {
            outboxPublisher.publishNext();
        } catch (RuntimeException ex) {
            log.error("Outbox publishing failed; will retry on a later run", ex);
        }
    }
}
