package com.neochat.chat.repository;

import com.neochat.chat.model.Conversation;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing conversations in MongoDB
 */
@ApplicationScoped
public class ConversationRepository implements ReactivePanacheMongoRepository<Conversation> {

    /**
     * Check if a user can access a conversation
     */
    public Uni<Boolean> canAccess(String userId, String conversationId) {
        return find("conversationId = ?1 and (participants in ?2 or overseers in ?2)", 
                    conversationId, List.of(userId))
                .firstResult()
                .map(conversation -> conversation != null);
    }

    /**
     * Create a new conversation
     */
    public Uni<Conversation> createConversation(List<String> participants, List<String> overseers) {
        String conversationId = UUID.randomUUID().toString();
        Conversation conversation = new Conversation(conversationId, participants, overseers);
        return persist(conversation);
    }

    /**
     * Get a conversation by ID
     */
    public Uni<Conversation> getConversation(String conversationId) {
        return find("conversationId", conversationId).firstResult();
    }

    /**
     * Update conversation last activity timestamp
     */
    public Uni<Void> updateLastActivity(String conversationId) {
        return find("conversationId", conversationId)
                .firstResult()
                .onItem().ifNotNull().transformToUni(conversation -> {
                    conversation.setUpdatedAt(Instant.now());
                    return update(conversation).replaceWithVoid();
                })
                .onItem().ifNull().failWith(() -> new RuntimeException("Conversation not found"));
    }

    /**
     * Get all conversations for a user
     */
    public Uni<List<Conversation>> getConversationsForUser(String userId) {
        return find("participants in ?1 or overseers in ?1", List.of(userId))
                .list();
    }
}
