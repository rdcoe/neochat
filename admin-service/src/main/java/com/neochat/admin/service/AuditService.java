package com.neochat.admin.service;

import com.neochat.admin.model.AuditLog;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;

/**
 * Service for managing audit logs
 */
@ApplicationScoped
public class AuditService {

    /**
     * Create an audit log entry
     */
    public Uni<Void> logAction(String userId, String action, String resourceType, 
                               String resourceId, String ipAddress, String userAgent, String status) {
        return Panache.withTransaction(() -> {
            AuditLog log = AuditLog.create(userId, action, resourceType, resourceId, status);
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            
            return Panache.getSession()
                    .flatMap(session -> session.persist(log))
                    .replaceWithVoid();
        });
    }

    /**
     * Get audit logs for a specific user
     */
    public Uni<List<AuditLog>> getUserAuditLogs(String userId, int page, int pageSize) {
        return AuditLog.<AuditLog>find("userId = ?1 ORDER BY timestamp DESC", userId)
                .page(page, pageSize)
                .list();
    }

    /**
     * Get audit logs for a specific resource
     */
    public Uni<List<AuditLog>> getResourceAuditLogs(String resourceType, String resourceId, int page, int pageSize) {
        return AuditLog.<AuditLog>find("resourceType = ?1 AND resourceId = ?2 ORDER BY timestamp DESC", 
                                       resourceType, resourceId)
                .page(page, pageSize)
                .list();
    }

    /**
     * Get audit logs within a time range
     */
    public Uni<List<AuditLog>> getAuditLogsByTimeRange(Instant from, Instant to, int page, int pageSize) {
        return AuditLog.<AuditLog>find("timestamp >= ?1 AND timestamp <= ?2 ORDER BY timestamp DESC", from, to)
                .page(page, pageSize)
                .list();
    }

    /**
     * Get audit logs by action
     */
    public Uni<List<AuditLog>> getAuditLogsByAction(String action, int page, int pageSize) {
        return AuditLog.<AuditLog>find("action = ?1 ORDER BY timestamp DESC", action)
                .page(page, pageSize)
                .list();
    }

    /**
     * Get failed actions audit logs
     */
    public Uni<List<AuditLog>> getFailedActions(int page, int pageSize) {
        return AuditLog.<AuditLog>find("status = 'failure' ORDER BY timestamp DESC")
                .page(page, pageSize)
                .list();
    }

    /**
     * Delete old audit logs (for cleanup)
     */
    public Uni<Long> deleteOldAuditLogs(Instant olderThan) {
        return Panache.withTransaction(() -> 
            AuditLog.delete("timestamp < ?1", olderThan)
        );
    }
}
