package io.github.mksfilmoteka.catalog.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    Optional<OutboxEvent> findFirstByPublishedTsIsNullOrderByCreatedTsAscIdAsc();

    @Modifying
    @Query("delete from OutboxEvent event where event.publishedTs < :cutoff")
    int deletePublishedBefore(Instant cutoff);
}
