package com.neochat.chat.service;

import com.neochat.chat.model.Conversation;
import com.neochat.chat.repository.ConversationRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Service for managing conversations
 */
@ApplicationScoped
public class ConversationService {

    @Inject
    ConversationRepository conversationRepository;

    /**
     * Create a new conversation
     */
    public Uni<Conversation> createConversation(List<String> participants, List<String> overseers) {
        return conversationRepository.createConversation(participants, overseers);
    }

    /**
     * Get conversation details
     */
    public Uni<Conversation> getConversation(String conversationId) {
        return conversationRepository.getConversation(conversationId);
    }

    /**
     * Get all conversations for a user
     */
    public Uni<List<Conversation>> getUserConversations(String userId) {
        return conversationRepository.getConversationsForUser(userId);
    }
}
