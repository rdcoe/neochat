package com.neochat.common.storage;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    @Inject
    PostgresEventRepository eventRepository;

    @Override
    public Uni<Void> storeEvent(String conversationId, String eventId, String eventType,
            String payload, String userId, Instant timestamp) {
        var entity = new PostgresEventEntity();
        entity.setId(UUID.fromString(eventId));
        entity.setConversationId(conversationId);
        entity.setEventType(eventType);
        entity.setPayload(payload);
        entity.setUserId(userId);
        entity.setTimestamp(timestamp);

        return Panache.withTransaction(() -> Panache.getSession()
                .flatMap(session -> session.persist(entity))
                .replaceWithVoid());
    }

    @Override
    public Uni<List<ChatEventRecord>> getEvents(String conversationId, Instant from, Instant to) {
        return eventRepository
            .find(
                        "conversationId = ?1 and timestamp >= ?2 and timestamp <= ?3 ORDER BY timestamp",
                        conversationId, from, to)
                .list()
                .map(entities -> entities.stream()
                        .map(entity -> new ChatEventRecord(
                                entity.getId().toString(),
                                entity.getConversationId(),
                                entity.getEventType(),
                                entity.getPayload(),
                                entity.getUserId(),
                                entity.getTimestamp()))
                        .toList());
    }

    @Override
    public Uni<Long> deleteEventsOlderThan(Instant threshold) {
        return Panache.withTransaction(() -> eventRepository.delete("timestamp < ?1", threshold));
    }

    /**
     * PostgreSQL entity for storing chat events
     */
    @Entity
    @Table(name = "chat_events", schema = "chat")
    public static class PostgresEventEntity extends PanacheEntityBase {

        @Id
        @Column(name = "id")
        private UUID id;

        @Column(name = "conversation_id", nullable = false)
        private String conversationId;

        @Column(name = "event_type", nullable = false)
        private String eventType;

        @Column(name = "payload", columnDefinition = "TEXT")
        private String payload;

        @Column(name = "user_id", nullable = false)
        private String userId;

        @Column(name = "timestamp", nullable = false)
        private Instant timestamp;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }
}
