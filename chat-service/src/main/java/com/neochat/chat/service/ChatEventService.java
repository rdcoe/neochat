package com.neochat.chat.service;

import com.neochat.chat.model.ChatEvent;
import com.neochat.chat.producer.KafkaEventProducer;
import com.neochat.chat.repository.ConversationRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Service for handling chat event business logic
 */
@ApplicationScoped
public class ChatEventService {
    private static final Logger LOG = Logger.getLogger(ChatEventService.class);

    private final ConversationRepository conversationRepository;
    private final KafkaEventProducer kafkaEventProducer;

    @Inject
    public ChatEventService(ConversationRepository conversationRepository,
            KafkaEventProducer kafkaEventProducer) {
        this.conversationRepository = conversationRepository;
        this.kafkaEventProducer = kafkaEventProducer;
    }

    /**
     * Post a message to a conversation
     */
    public Uni<Void> postMessage(String conversationId, String userId, String content, String messageType) {
        // Validate access
        return conversationRepository.canAccess(userId, conversationId)
                .flatMap(canAccess -> {
                    if (!canAccess) {
                        LOG.warnf("User %s attempted to access conversation %s without permission", 
                                 userId, conversationId);
                        return Uni.createFrom().failure(
                            new SecurityException("User does not have access to this conversation")
                        );
                    }

                    // Create chat event
                    ChatEvent event = ChatEvent.create(
                        conversationId,
                        messageType != null ? messageType : "message",
                        content,
                        userId
                    );

                    // Publish to Kafka
                    return kafkaEventProducer.publishEvent(event)
                            .invoke(() -> LOG.debugf("Message posted to conversation %s", conversationId));
                });
    }

    /**
     * Validate conversation access
     */
    public Uni<Boolean> validateAccess(String userId, String conversationId) {
        return conversationRepository.canAccess(userId, conversationId);
    }
}
