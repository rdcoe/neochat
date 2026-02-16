package com.neochat.admin.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Set;

/**
 * User entity stored in MongoDB
 */
@MongoEntity(collection = "users")
public class User extends ReactivePanacheMongoEntity {
    public ObjectId id;
    public String userId;
    public String email;
    public String name;
    public Set<String> roles;
    public Set<String> groups;
    public String status;
    public Instant createdAt;
    public Instant updatedAt;

    public User() {
    }

    public User(String userId, String email, String name, Set<String> roles, Set<String> groups) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.roles = roles;
        this.groups = groups;
        this.status = "active";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
