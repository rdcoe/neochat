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
        document.eventId = eventId;
        document.conversationId = conversationId;
        document.eventType = eventType;
        document.payload = payload;
        document.userId = userId;
        document.timestamp = timestamp;
        document.createdAt = Instant.now();

        return persist(document).replaceWithVoid();
    }

    @Override
    public Uni<List<ChatEventRecord>> getEvents(String conversationId, Instant from, Instant to) {
        return find("conversationId = ?1 and timestamp >= ?2 and timestamp <= ?3", 
                    conversationId, from, to)
                .stream()
                .map(doc -> new ChatEventRecord(
                    doc.eventId,
                    doc.conversationId,
                    doc.eventType,
                    doc.payload,
                    doc.userId,
                    doc.timestamp
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
        public ObjectId id;
        public String eventId;
        public String conversationId;
        public String eventType;
        public String payload;
        public String userId;
        public Instant timestamp;
        public Instant createdAt; // For TTL index
    }
}
