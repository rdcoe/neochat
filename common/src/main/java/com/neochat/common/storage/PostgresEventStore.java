package com.neochat.common.storage;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PostgreSQL implementation of EventStore
 * Stores all events with partitioning by date
 */
@ApplicationScoped
public class PostgresEventStore implements EventStore {

    @Override
    public Uni<Void> storeEvent(String conversationId, String eventId, String eventType,
                                String payload, String userId, Instant timestamp) {
        var entity = new PostgresEventEntity();
        entity.id = UUID.fromString(eventId);
        entity.conversationId = conversationId;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.userId = userId;
        entity.timestamp = timestamp;

        return Panache.withTransaction(() -> 
            Panache.getSession()
                .flatMap(session -> session.persist(entity))
                .replaceWithVoid()
        );
    }

    @Override
    public Uni<List<ChatEventRecord>> getEvents(String conversationId, Instant from, Instant to) {
        return PostgresEventEntity
                .<PostgresEventEntity>find(
                    "conversationId = ?1 and timestamp >= ?2 and timestamp <= ?3 ORDER BY timestamp", 
                    conversationId, from, to
                )
                .list()
                .map(entities -> entities.stream()
                    .map(entity -> new ChatEventRecord(
                        entity.id.toString(),
                        entity.conversationId,
                        entity.eventType,
                        entity.payload,
                        entity.userId,
                        entity.timestamp
                    ))
                    .toList()
                );
    }

    @Override
    public Uni<Long> deleteEventsOlderThan(Instant threshold) {
        return Panache.withTransaction(() ->
            PostgresEventEntity.delete("timestamp < ?1", threshold)
        );
    }

    /**
     * PostgreSQL entity for storing chat events
     */
    @Entity
    @Table(name = "chat_events", schema = "chat")
    public static class PostgresEventEntity extends PanacheEntityBase {
        
        @Id
        @Column(name = "id")
        public UUID id;

        @Column(name = "conversation_id", nullable = false)
        public String conversationId;

        @Column(name = "event_type", nullable = false)
        public String eventType;

        @Column(name = "payload", columnDefinition = "TEXT")
        public String payload;

        @Column(name = "user_id", nullable = false)
        public String userId;

        @Column(name = "timestamp", nullable = false)
        public Instant timestamp;
    }
}
