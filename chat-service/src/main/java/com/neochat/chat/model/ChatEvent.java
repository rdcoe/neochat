package com.neochat.chat.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable chat event
 */
public record ChatEvent(
    String eventId,
    String conversationId,
    String eventType,
    String payload,
    String userId,
    Instant timestamp
) {
    public ChatEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static ChatEvent create(String conversationId, String eventType, String payload, String userId) {
        return new ChatEvent(null, conversationId, eventType, payload, userId, null);
    }
}
