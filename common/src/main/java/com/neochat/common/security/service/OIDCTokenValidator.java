package com.neochat.common.security.service;

import io.quarkus.oidc.OidcSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Validates and manages OIDC tokens
 */
@ApplicationScoped
public class OIDCTokenValidator {
    private static final Logger LOG = Logger.getLogger(OIDCTokenValidator.class);

    @Inject
    Optional<OidcSession> oidcSession;

    /**
     * Get the current OIDC ID token
     */
    public Optional<String> getIdToken() {
        if (oidcSession.isEmpty()) {
            return Optional.empty();
        }
        
        String idToken = oidcSession.get().getIdToken().getRawToken();
        return Optional.ofNullable(idToken);
    }

    /**
     * Validate if OIDC session is active
     */
    public boolean isSessionActive() {
        return oidcSession.isPresent();
    }

    /**
     * Get the subject from OIDC session
     */
    public Optional<String> getSubject() {
        if (oidcSession.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(oidcSession.get().getIdToken().getSubject());
    }
}
