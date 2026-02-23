package com.neochat.common.security.service;

import com.neochat.common.security.model.IdentityTokenClaims;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates JWT tokens and extracts claims
 */
@ApplicationScoped
public class TokenValidator {
    private static final Logger LOG = Logger.getLogger(TokenValidator.class);

    private final JWTParser jwtParser;
    private final Instance<JwtVerificationKeySource> verificationKeySource;

    public TokenValidator(JWTParser jwtParser, Instance<JwtVerificationKeySource> verificationKeySource) {
        this.jwtParser = jwtParser;
        this.verificationKeySource = verificationKeySource;
    }

    /**
     * Validate and parse a JWT token
     */
    public JsonWebToken validateToken(String token) throws ParseException {
        if (verificationKeySource.isResolvable()) {
            return jwtParser.verify(token, verificationKeySource.get().getVerificationKey());
        }
        return jwtParser.parse(token);
    }

    /**
     * Extract identity token claims from a JWT
     */
    public IdentityTokenClaims extractIdentityClaims(String token) {
        try {
            JsonWebToken jwt = validateToken(token);

            String subject = jwt.getSubject();
            String email = jwt.getClaim("email");

            Set<String> roles = new HashSet<>();
            if (jwt.getGroups() != null) {
                roles.addAll(jwt.getGroups());
            }

            Set<String> groups = new HashSet<>();
            Object groupsClaim = jwt.getClaim("groups");
            if (groupsClaim instanceof Set<?>) {
                @SuppressWarnings("unchecked")
                Set<String> castedGroups = (Set<String>) groupsClaim;
                groups.addAll(castedGroups);
            }

            return new IdentityTokenClaims(subject, email, roles, groups);
        } catch (ParseException e) {
            LOG.error("Failed to parse token", e);
            throw new NotAuthorizedException("Invalid token", e);
        }
    }

    /**
     * Validate token signature
     */
    public boolean isValidSignature(String token) {
        try {
            validateToken(token);
            return true;
        } catch (Exception e) {
            LOG.debugf("Token signature validation failed: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token has required scopes
     */
    public boolean hasScopes(JsonWebToken jwt, Set<String> requiredScopes) {
        if (requiredScopes == null || requiredScopes.isEmpty()) {
            return true;
        }

        String scopeClaim = jwt.getClaim("scope");
        if (scopeClaim == null) {
            return false;
        }

        Set<String> tokenScopes = Set.of(scopeClaim.split("\\s+"));
        return tokenScopes.containsAll(requiredScopes);
    }
}
