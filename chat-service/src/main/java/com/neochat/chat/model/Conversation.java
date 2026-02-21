package com.neochat.chat.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;

/**
 * Conversation entity stored in MongoDB
 */
@MongoEntity(collection = "conversations")
public class Conversation extends ReactivePanacheMongoEntity {
    private ObjectId id;
    private String conversationId;
    private List<String> participants;
    private List<String> overseers;
    private Instant createdAt;
    private Instant updatedAt;
    private String status;

    public Conversation() {
    }

    public Conversation(String conversationId, List<String> participants, List<String> overseers) {
        this.conversationId = conversationId;
        this.participants = participants;
        this.overseers = overseers;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = "active";
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public List<String> getOverseers() {
        return overseers;
    }

    public void setOverseers(List<String> overseers) {
        this.overseers = overseers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
