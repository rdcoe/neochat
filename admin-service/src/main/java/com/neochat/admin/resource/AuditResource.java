package com.neochat.admin.resource;

import com.neochat.admin.model.AuditLog;
import com.neochat.admin.service.AuditService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;

/**
 * REST resource for audit operations
 */
@Path("/api/audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"admin", "overseer"})
public class AuditResource {

    @Inject
    AuditService auditService;

    /**
     * Get audit logs for a user
     */
    @GET
    @Path("/users/{userId}")
    public Uni<List<AuditLog>> getUserAuditLogs(
            @PathParam("userId") String userId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("50") int pageSize) {
        return auditService.getUserAuditLogs(userId, page, pageSize);
    }

    /**
     * Get audit logs for a resource
     */
    @GET
    @Path("/resources/{resourceType}/{resourceId}")
    public Uni<List<AuditLog>> getResourceAuditLogs(
            @PathParam("resourceType") String resourceType,
            @PathParam("resourceId") String resourceId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("50") int pageSize) {
        return auditService.getResourceAuditLogs(resourceType, resourceId, page, pageSize);
    }

    /**
     * Get audit logs by time range
     */
    @GET
    @Path("/time-range")
    public Uni<List<AuditLog>> getAuditLogsByTimeRange(
            @QueryParam("from") long fromTimestamp,
            @QueryParam("to") long toTimestamp,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("50") int pageSize) {
        Instant from = Instant.ofEpochMilli(fromTimestamp);
        Instant to = Instant.ofEpochMilli(toTimestamp);
        return auditService.getAuditLogsByTimeRange(from, to, page, pageSize);
    }

    /**
     * Get audit logs by action
     */
    @GET
    @Path("/actions/{action}")
    public Uni<List<AuditLog>> getAuditLogsByAction(
            @PathParam("action") String action,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("50") int pageSize) {
        return auditService.getAuditLogsByAction(action, page, pageSize);
    }

    /**
     * Get failed actions
     */
    @GET
    @Path("/failed")
    public Uni<List<AuditLog>> getFailedActions(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("50") int pageSize) {
        return auditService.getFailedActions(page, pageSize);
    }
}
