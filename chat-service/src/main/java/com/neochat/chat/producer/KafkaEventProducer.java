package com.neochat.chat.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neochat.chat.model.ChatEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

/**
 * Reactive Kafka producer for chat events
 */
@ApplicationScoped
public class KafkaEventProducer {
    private static final Logger LOG = Logger.getLogger(KafkaEventProducer.class);

    private final Emitter<KafkaRecord<String, String>> emitter;
    private final ObjectMapper objectMapper;

    @Inject
    public KafkaEventProducer(@Channel("chat-events-out") Emitter<KafkaRecord<String, String>> emitter,
            ObjectMapper objectMapper) {
        this.emitter = emitter;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish chat event to Kafka
     * Uses conversation_id as partition key for ordering
     */
    public Uni<Void> publishEvent(ChatEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            // Use conversationId as partition key to ensure ordering
            KafkaRecord<String, String> kafkarecord = KafkaRecord.of(
                    event.conversationId(),
                    payload);

            LOG.debugf("Publishing event %s to conversation %s", event.eventId(), event.conversationId());

            return Uni.createFrom().completionStage(emitter.send(kafkarecord))
                    .replaceWithVoid();

        } catch (Exception e) {
            LOG.error("Failed to publish event to Kafka", e);
            return Uni.createFrom().failure(e);
        }
    }
}
