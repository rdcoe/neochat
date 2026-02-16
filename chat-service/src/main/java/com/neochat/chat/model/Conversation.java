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
    public ObjectId id;
    public String conversationId;
    public List<String> participants;
    public List<String> overseers;
    public Instant createdAt;
    public Instant updatedAt;
    public String status;

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
}
