package com.neochat.admin.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Audit log entity stored in PostgreSQL
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends PanacheEntityBase {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "timestamp", nullable = false)
    public Instant timestamp;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "action", nullable = false)
    public String action;

    @Column(name = "resource_type", nullable = false)
    public String resourceType;

    @Column(name = "resource_id")
    public String resourceId;

    @Column(name = "ip_address")
    public String ipAddress;

    @Column(name = "user_agent")
    public String userAgent;

    @Column(name = "status", nullable = false)
    public String status;

    @Column(name = "details", columnDefinition = "TEXT")
    public String details;

    public AuditLog() {
        this.id = UUID.randomUUID();
        this.timestamp = Instant.now();
    }

    public static AuditLog create(String userId, String action, String resourceType, 
                                  String resourceId, String status) {
        AuditLog log = new AuditLog();
        log.userId = userId;
        log.action = action;
        log.resourceType = resourceType;
        log.resourceId = resourceId;
        log.status = status;
        return log;
    }
}
