package com.neochat.admin.resource;

import com.neochat.admin.model.User;
import com.neochat.admin.service.AuditService;
import com.neochat.admin.service.UserManagementService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Set;

/**
 * REST resource for admin operations
 */
@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    UserManagementService userManagementService;

    @Inject
    AuditService auditService;

    @Context
    SecurityContext securityContext;

    /**
     * Create a new user
     */
    @POST
    @Path("/users")
    @RolesAllowed("admin")
    public Uni<Response> createUser(CreateUserRequest request) {
        return userManagementService
                .createUser(request.userId, request.email, request.name, request.roles, request.groups)
                .flatMap(user -> 
                    auditService.logAction(
                        securityContext.getUserPrincipal().getName(),
                        "CREATE_USER",
                        "user",
                        user.userId,
                        null,
                        null,
                        "success"
                    ).replaceWith(Response.status(Response.Status.CREATED).entity(user).build())
                )
                .onFailure().recoverWithItem(t -> 
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorResponse(t.getMessage()))
                            .build()
                );
    }

    /**
     * Get user by ID
     */
    @GET
    @Path("/users/{userId}")
    @RolesAllowed({"admin", "overseer"})
    public Uni<Response> getUser(@PathParam("userId") String userId) {
        return userManagementService.getUser(userId)
                .onItem().ifNotNull().transform(user -> Response.ok(user).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * List all users
     */
    @GET
    @Path("/users")
    @RolesAllowed({"admin", "overseer"})
    public Uni<List<User>> listUsers(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        return userManagementService.listUsers(page, pageSize);
    }

    /**
     * Update user roles
     */
    @PUT
    @Path("/users/{userId}/roles")
    @RolesAllowed("admin")
    public Uni<Response> updateUserRoles(@PathParam("userId") String userId, UpdateRolesRequest request) {
        return userManagementService.updateUserRoles(userId, request.roles)
                .flatMap(user -> 
                    auditService.logAction(
                        securityContext.getUserPrincipal().getName(),
                        "UPDATE_USER_ROLES",
                        "user",
                        userId,
                        null,
                        null,
                        "success"
                    ).replaceWith(Response.ok(user).build())
                )
                .onFailure().recoverWithItem(t -> 
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorResponse(t.getMessage()))
                            .build()
                );
    }

    /**
     * Update user status
     */
    @PUT
    @Path("/users/{userId}/status")
    @RolesAllowed("admin")
    public Uni<Response> updateUserStatus(@PathParam("userId") String userId, UpdateStatusRequest request) {
        return userManagementService.updateUserStatus(userId, request.status)
                .flatMap(user -> 
                    auditService.logAction(
                        securityContext.getUserPrincipal().getName(),
                        "UPDATE_USER_STATUS",
                        "user",
                        userId,
                        null,
                        null,
                        "success"
                    ).replaceWith(Response.ok(user).build())
                )
                .onFailure().recoverWithItem(t -> 
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorResponse(t.getMessage()))
                            .build()
                );
    }

    /**
     * Delete user
     */
    @DELETE
    @Path("/users/{userId}")
    @RolesAllowed("admin")
    public Uni<Response> deleteUser(@PathParam("userId") String userId) {
        return userManagementService.deleteUser(userId)
                .flatMap(deleted -> 
                    auditService.logAction(
                        securityContext.getUserPrincipal().getName(),
                        "DELETE_USER",
                        "user",
                        userId,
                        null,
                        null,
                        deleted ? "success" : "failure"
                    ).replaceWith(deleted ? Response.noContent().build() : 
                                 Response.status(Response.Status.NOT_FOUND).build())
                )
                .onFailure().recoverWithItem(t -> 
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorResponse(t.getMessage()))
                            .build()
                );
    }

    // Request/Response DTOs
    public static record CreateUserRequest(
        String userId,
        String email,
        String name,
        Set<String> roles,
        Set<String> groups
    ) {}

    public static record UpdateRolesRequest(Set<String> roles) {}
    public static record UpdateStatusRequest(String status) {}
    public static record ErrorResponse(String error) {}
}
