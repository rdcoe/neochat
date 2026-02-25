package com.neochat.common.security.filter;

import com.neochat.common.security.model.IdentityTokenClaims;
import com.neochat.common.security.service.TokenValidator;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Set;

/**
 * JWT Authorization Filter
 * Validates Bearer tokens and checks scopes
 * Priority: AUTHENTICATION (1000)
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JWTAuthZFilter implements ContainerRequestFilter {
    private static final Logger LOG = Logger.getLogger(JWTAuthZFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_CLAIMS_PROPERTY = "token.claims";

    @ConfigProperty(name = "auth.jwt.enabled", defaultValue = "true")
    boolean jwtAuthEnabled;

    private final TokenValidator tokenValidator;

    @Inject
    public JWTAuthZFilter(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!jwtAuthEnabled) {
            LOG.debug("JWT authentication is disabled");
            return;
        }

        // Skip for health and metrics endpoints
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("/q/health") || path.startsWith("/q/metrics")) {
            return;
        }

        String authHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            LOG.debug("No Bearer token found in Authorization header");
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Missing or invalid Authorization header")
                    .build()
            );
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            JsonWebToken jwt = tokenValidator.validateToken(token);
            
            // Check if token has required scopes (example: chat:write, chat:read)
            Set<String> requiredScopes = extractRequiredScopes(requestContext);
            if (!tokenValidator.hasScopes(jwt, requiredScopes)) {
                LOG.warnf("Token missing required scopes: %s", requiredScopes);
                requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                        .entity("Insufficient scopes")
                        .build()
                );
                return;
            }

            // Attach token claims to request context for downstream filters
            IdentityTokenClaims claims = tokenValidator.extractIdentityClaims(token);
            requestContext.setProperty(TOKEN_CLAIMS_PROPERTY, claims);
            
            LOG.debugf("JWT validated for subject: %s", jwt.getSubject());
            
        } catch (ParseException e) {
            LOG.error("Token validation failed", e);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token")
                    .build()
            );
        }
    }

    /**
     * Extract required scopes from the request (could be from annotations, path, etc.)
     * This is a simplified implementation
     */
    private Set<String> extractRequiredScopes(ContainerRequestContext requestContext) {
        // In a real implementation, you'd extract this from @Scopes annotation or similar
        // For now, return empty set (no specific scope requirements)
        return Set.of();
    }
}
