package com.neochat.common.storage;

import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.List;

/**
 * Interface for event storage (can be implemented for MongoDB, PostgreSQL, etc.)
 */
public interface EventStore {
    
    /**
     * Store a chat event
     */
    Uni<Void> storeEvent(String conversationId, String eventId, String eventType, 
                         String payload, String userId, Instant timestamp);

    /**
     * Retrieve events for a conversation within a time range
     */
    Uni<List<ChatEventRecord>> getEvents(String conversationId, Instant from, Instant to);

    /**
     * Delete events older than specified time (for cleanup/TTL)
     */
    Uni<Long> deleteEventsOlderThan(Instant threshold);

    /**
     * Record class for chat events
     */
    record ChatEventRecord(
        String eventId,
        String conversationId,
        String eventType,
        String payload,
        String userId,
        Instant timestamp
    ) {}
}
