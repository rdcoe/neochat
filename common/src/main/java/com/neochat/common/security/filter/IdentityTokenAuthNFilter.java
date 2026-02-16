package com.neochat.common.security.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neochat.common.security.model.IdentityTokenClaims;
import com.neochat.common.security.service.OIDCTokenValidator;
import com.neochat.common.security.service.TokenService;
import com.neochat.common.security.service.TokenValidator;
import jakarta.annotation.Priority;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Identity Token Authentication Filter
 * Validates identity tokens and checks role-based access
 * Priority: AUTHENTICATION + 1 (runs after JWTAuthZFilter)
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 1)
public class IdentityTokenAuthNFilter implements ContainerRequestFilter {
    private static final Logger LOG = Logger.getLogger(IdentityTokenAuthNFilter.class);
    private static final String IDENTITY_TOKEN_HEADER = "X-Identity-Token";
    private static final String IDENTITY_CLAIMS_PROPERTY = "identity.claims";

    @Inject
    TokenValidator tokenValidator;

    @Inject
    TokenService tokenService;

    @Inject
    OIDCTokenValidator oidcTokenValidator;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Skip for health and metrics endpoints
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("/q/health") || path.startsWith("/q/metrics")) {
            return;
        }

        String identityToken = extractIdentityToken(requestContext);

        IdentityTokenClaims claims = null;

        if (identityToken != null) {
            // Validate existing identity token
            if (tokenValidator.isValidSignature(identityToken)) {
                claims = tokenValidator.extractIdentityClaims(identityToken);
                LOG.debugf("Valid identity token for subject: %s", claims.getSubject());
            } else {
                LOG.warn("Invalid identity token signature, attempting to regenerate");
            }
        }

        // If no valid identity token, try to regenerate from OIDC
        if (claims == null) {
            var oidcIdToken = oidcTokenValidator.getIdToken();
            if (oidcIdToken.isPresent()) {
                LOG.debug("Regenerating identity token from OIDC");
                claims = tokenValidator.extractIdentityClaims(oidcIdToken.get());
                
                // Generate new identity token
                String newToken = tokenService.signIdentityToken(
                    claims.getSubject(),
                    claims.getEmail(),
                    claims.getRoles(),
                    claims.getGroups()
                );
                
                // Attach new token to response header for client to cache
                requestContext.setProperty("new_identity_token", newToken);
            } else {
                LOG.warn("No valid identity token and no OIDC session");
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                        .entity("No valid identity")
                        .build()
                );
                return;
            }
        }

        // Check role-based access
        if (!hasRequiredRoles(claims)) {
            LOG.warnf("User %s does not have required roles", claims.getSubject());
            requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                    .entity("Insufficient permissions")
                    .build()
            );
            return;
        }

        // Attach identity claims to request context
        requestContext.setProperty(IDENTITY_CLAIMS_PROPERTY, claims);
    }

    /**
     * Extract identity token from request body, header, or OIDC
     */
    private String extractIdentityToken(ContainerRequestContext requestContext) throws IOException {
        // 1. Try request body (for JSON payloads)
        if (requestContext.hasEntity() && requestContext.getMediaType() != null 
                && requestContext.getMediaType().toString().contains("application/json")) {
            
            InputStream originalStream = requestContext.getEntityStream();
            String body = new String(originalStream.readAllBytes(), StandardCharsets.UTF_8);
            
            // Reset the stream for downstream processing
            requestContext.setEntityStream(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
            
            try {
                JsonNode jsonNode = objectMapper.readTree(body);
                if (jsonNode.has("identity_token")) {
                    return jsonNode.get("identity_token").asText();
                }
            } catch (Exception e) {
                LOG.debug("Failed to parse JSON body for identity_token", e);
            }
        }

        // 2. Try X-Identity-Token header
        String headerToken = requestContext.getHeaderString(IDENTITY_TOKEN_HEADER);
        if (headerToken != null && !headerToken.isEmpty()) {
            return headerToken;
        }

        // 3. Will fall back to OIDC in main filter method
        return null;
    }

    /**
     * Check if user has required roles based on @RolesAllowed annotation
     */
    private boolean hasRequiredRoles(IdentityTokenClaims claims) {
        Method resourceMethod = resourceInfo.getResourceMethod();
        if (resourceMethod == null) {
            return true; // No method means no role requirement
        }

        RolesAllowed rolesAllowed = resourceMethod.getAnnotation(RolesAllowed.class);
        if (rolesAllowed == null) {
            // Check class-level annotation
            Class<?> resourceClass = resourceInfo.getResourceClass();
            rolesAllowed = resourceClass.getAnnotation(RolesAllowed.class);
        }

        if (rolesAllowed == null) {
            return true; // No role requirement
        }

        Set<String> requiredRoles = new HashSet<>(Arrays.asList(rolesAllowed.value()));
        Set<String> userRoles = claims.getRoles();

        return userRoles.stream().anyMatch(requiredRoles::contains);
    }
}
