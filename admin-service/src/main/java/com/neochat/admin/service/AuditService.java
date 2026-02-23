package com.neochat.admin.service;

import java.time.Instant;
import java.util.List;

import com.neochat.admin.model.AuditLog;
import com.neochat.admin.repository.AuditLogRepository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for managing audit logs
 */
@ApplicationScoped
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

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
        return auditLogRepository.findByUserId(userId, page, pageSize);
    }

    /**
     * Get audit logs for a specific resource
     */
    public Uni<List<AuditLog>> getResourceAuditLogs(String resourceType, String resourceId, int page, int pageSize) {
        return auditLogRepository.findByResource(resourceType, resourceId, page, pageSize);
    }

    /**
     * Get audit logs within a time range
     */
    public Uni<List<AuditLog>> getAuditLogsByTimeRange(Instant from, Instant to, int page, int pageSize) {
        return auditLogRepository.findByTimeRange(from, to, page, pageSize);
    }

    /**
     * Get audit logs by action
     */
    public Uni<List<AuditLog>> getAuditLogsByAction(String action, int page, int pageSize) {
        return auditLogRepository.findByAction(action, page, pageSize);
    }

    /**
     * Get failed actions audit logs
     */
    public Uni<List<AuditLog>> getFailedActions(int page, int pageSize) {
        return auditLogRepository.findFailed(page, pageSize);
    }

    /**
     * Delete old audit logs (for cleanup)
     */
    public Uni<Long> deleteOldAuditLogs(Instant olderThan) {
        return Panache.withTransaction(() -> auditLogRepository.deleteOlderThan(olderThan));
    }
}
