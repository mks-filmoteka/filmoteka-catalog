package io.github.mksfilmoteka.catalog.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    Optional<OutboxEvent> findFirstByPublishedTsIsNullOrderByCreatedTsAscIdAsc();
}
