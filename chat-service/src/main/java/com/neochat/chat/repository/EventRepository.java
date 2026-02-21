package com.neochat.chat.repository;

import com.neochat.common.storage.EventStore;
import com.neochat.common.storage.MongoEventStore;
import com.neochat.common.storage.PostgresEventStore;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing chat events across MongoDB and PostgreSQL
 */
@ApplicationScoped
public class EventRepository {

    @Inject
    MongoEventStore mongoEventStore;

    @Inject
    PostgresEventStore postgresEventStore;

    /**
     * Get events for a conversation from both stores
     * MongoDB for recent (hot), PostgreSQL for historical (cold)
     */
    public Uni<List<EventStore.ChatEventRecord>> getEventsForConversation(
            String conversationId, Instant from, Instant to) {

        Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * (long) 60);

        // If querying recent data (within 30 days), use MongoDB
        if (from.isAfter(thirtyDaysAgo)) {
            return mongoEventStore.getEvents(conversationId, from, to);
        }

        // If querying historical data, use PostgreSQL
        if (to.isBefore(thirtyDaysAgo)) {
            return postgresEventStore.getEvents(conversationId, from, to);
        }

        // If spanning both hot and cold storage, query both and merge
        return Uni.combine().all()
                .unis(
                        mongoEventStore.getEvents(conversationId, thirtyDaysAgo, to),
                        postgresEventStore.getEvents(conversationId, from, thirtyDaysAgo))
                .with((mongoEvents, postgresEvents) -> {
                    List<EventStore.ChatEventRecord> allEvents = new ArrayList<>();
                    allEvents.addAll(postgresEvents);
                    allEvents.addAll(mongoEvents);
                    return allEvents.stream()
                            .sorted((e1, e2) -> e1.timestamp().compareTo(e2.timestamp()))
                            .toList();
                });
    }

    /**
     * Get recent events for a conversation (last N events)
     */
    public Uni<List<EventStore.ChatEventRecord>> getRecentEvents(String conversationId, int limit) {
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * (long) 60);
        return mongoEventStore.getEvents(conversationId, thirtyDaysAgo, Instant.now())
                .map(events -> events.stream()
                        .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp()))
                        .limit(limit)
                        .toList());
    }
}
