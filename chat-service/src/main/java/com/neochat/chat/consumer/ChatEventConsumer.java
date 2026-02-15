package com.neochat.chat.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neochat.chat.model.ChatEvent;
import com.neochat.common.storage.MongoEventStore;
import com.neochat.common.storage.PostgresEventStore;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletionStage;

/**
 * Kafka consumer for chat events
 * Writes to both MongoDB (hot storage) and PostgreSQL (cold storage)
 */
@ApplicationScoped
public class ChatEventConsumer {
    private static final Logger LOG = Logger.getLogger(ChatEventConsumer.class);

    @Inject
    MongoEventStore mongoEventStore;

    @Inject
    PostgresEventStore postgresEventStore;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Consume chat events from Kafka
     * Store in both MongoDB (hot, TTL 30 days) and PostgreSQL (cold, permanent)
     */
    @Incoming("chat-events-in")
    public CompletionStage<Void> consumeEvent(Message<String> message) {
        try {
            String payload = message.getPayload();
            ChatEvent event = objectMapper.readValue(payload, ChatEvent.class);

            LOG.debugf("Consuming event %s for conversation %s", event.eventId(), event.conversationId());

            // Store in both MongoDB and PostgreSQL
            return Uni.combine().all()
                    .unis(
                        mongoEventStore.storeEvent(
                            event.conversationId(),
                            event.eventId(),
                            event.eventType(),
                            event.payload(),
                            event.userId(),
                            event.timestamp()
                        ),
                        postgresEventStore.storeEvent(
                            event.conversationId(),
                            event.eventId(),
                            event.eventType(),
                            event.payload(),
                            event.userId(),
                            event.timestamp()
                        )
                    )
                    .discardItems()
                    .invoke(() -> LOG.debugf("Event %s stored successfully", event.eventId()))
                    .onFailure().invoke(t -> LOG.error("Failed to store event", t))
                    .replaceWith(message.ack())
                    .subscribeAsCompletionStage();

        } catch (Exception e) {
            LOG.error("Failed to process event", e);
            return message.nack(e).subscribeAsCompletionStage();
        }
    }
}
