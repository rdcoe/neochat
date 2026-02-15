package com.neochat.admin.service;

import com.neochat.admin.model.User;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Service for managing users
 */
@ApplicationScoped
public class UserManagementService implements ReactivePanacheMongoRepository<User> {

    /**
     * Create a new user
     */
    public Uni<User> createUser(String userId, String email, String name, Set<String> roles, Set<String> groups) {
        User user = new User(userId, email, name, roles, groups);
        return persist(user);
    }

    /**
     * Get user by ID
     */
    public Uni<User> getUser(String userId) {
        return find("userId", userId).firstResult();
    }

    /**
     * Update user roles
     */
    public Uni<User> updateUserRoles(String userId, Set<String> roles) {
        return find("userId", userId)
                .firstResult()
                .onItem().ifNotNull().transformToUni(user -> {
                    user.roles = roles;
                    user.updatedAt = Instant.now();
                    return update(user);
                });
    }

    /**
     * Update user groups
     */
    public Uni<User> updateUserGroups(String userId, Set<String> groups) {
        return find("userId", userId)
                .firstResult()
                .onItem().ifNotNull().transformToUni(user -> {
                    user.groups = groups;
                    user.updatedAt = Instant.now();
                    return update(user);
                });
    }

    /**
     * Update user status
     */
    public Uni<User> updateUserStatus(String userId, String status) {
        return find("userId", userId)
                .firstResult()
                .onItem().ifNotNull().transformToUni(user -> {
                    user.status = status;
                    user.updatedAt = Instant.now();
                    return update(user);
                });
    }

    /**
     * List all users
     */
    public Uni<List<User>> listUsers(int page, int pageSize) {
        return findAll()
                .page(page, pageSize)
                .list();
    }

    /**
     * Search users by email
     */
    public Uni<List<User>> searchByEmail(String email) {
        return find("email", email).list();
    }

    /**
     * Get users by role
     */
    public Uni<List<User>> getUsersByRole(String role) {
        return find("roles", role).list();
    }

    /**
     * Delete user
     */
    public Uni<Boolean> deleteUser(String userId) {
        return delete("userId", userId)
                .map(count -> count > 0);
    }
}
