package com.neochat.common.storage;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB implementation of EventStore
 * Stores recent events with TTL (30 days)
 */
@ApplicationScoped
public class MongoEventStore implements EventStore, ReactivePanacheMongoRepository<MongoEventStore.MongoEventDocument> {

    @Override
    public Uni<Void> storeEvent(String conversationId, String eventId, String eventType,
                                String payload, String userId, Instant timestamp) {
        var document = new MongoEventDocument();
        document.setEventId(eventId);
        document.setConversationId(conversationId);
        document.setEventType(eventType);
        document.setPayload(payload);
        document.setUserId(userId);
        document.setTimestamp(timestamp);
        document.setCreatedAt(Instant.now());

        return persist(document).replaceWithVoid();
    }

    @Override
    public Uni<List<ChatEventRecord>> getEvents(String conversationId, Instant from, Instant to) {
        return find("conversationId = ?1 and timestamp >= ?2 and timestamp <= ?3", 
                    conversationId, from, to)
                .stream()
                .map(doc -> new ChatEventRecord(
                    doc.getEventId(),
                    doc.getConversationId(),
                    doc.getEventType(),
                    doc.getPayload(),
                    doc.getUserId(),
                    doc.getTimestamp()
                ))
                .collect().asList();
    }

    @Override
    public Uni<Long> deleteEventsOlderThan(Instant threshold) {
        return delete("timestamp < ?1", threshold);
    }

    /**
     * MongoDB document for storing chat events
     */
    public static class MongoEventDocument extends ReactivePanacheMongoEntity {
        private ObjectId id;
        private String eventId;
        private String conversationId;
        private String eventType;
        private String payload;
        private String userId;
        private Instant timestamp;
        private Instant createdAt; // For TTL index

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
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

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }
}
