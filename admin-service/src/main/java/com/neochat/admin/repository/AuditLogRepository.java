package com.neochat.admin.repository;

import com.neochat.admin.model.AuditLog;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AuditLogRepository implements PanacheRepositoryBase<AuditLog, UUID> {

    public Uni<List<AuditLog>> findByUserId(String userId, int page, int pageSize) {
        return find("userId = ?1 ORDER BY timestamp DESC", userId)
                .page(page, pageSize)
                .list();
    }

    public Uni<List<AuditLog>> findByResource(String resourceType, String resourceId, int page, int pageSize) {
        return find("resourceType = ?1 AND resourceId = ?2 ORDER BY timestamp DESC", resourceType, resourceId)
                .page(page, pageSize)
                .list();
    }

    public Uni<List<AuditLog>> findByTimeRange(Instant from, Instant to, int page, int pageSize) {
        return find("timestamp >= ?1 AND timestamp <= ?2 ORDER BY timestamp DESC", from, to)
                .page(page, pageSize)
                .list();
    }

    public Uni<List<AuditLog>> findByAction(String action, int page, int pageSize) {
        return find("action = ?1 ORDER BY timestamp DESC", action)
                .page(page, pageSize)
                .list();
    }

    public Uni<List<AuditLog>> findFailed(int page, int pageSize) {
        return find("status = 'failure' ORDER BY timestamp DESC")
                .page(page, pageSize)
                .list();
    }

    public Uni<Long> deleteOlderThan(Instant olderThan) {
        return delete("timestamp < ?1", olderThan);
    }
}