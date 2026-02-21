package com.neochat.admin.model;

import java.time.Instant;
import java.util.Set;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

/**
 * User entity stored in MongoDB
 */
@MongoEntity(collection = "users")
public class User extends ReactivePanacheMongoEntity {
    private String userId;
    private String email;
    private String name;
    private Set<String> roles;
    private Set<String> groups;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
