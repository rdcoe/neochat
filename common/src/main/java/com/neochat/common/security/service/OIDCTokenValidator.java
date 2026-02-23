package com.neochat.common.security.service;

import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OidcSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

/**
 * Validates and manages OIDC tokens
 */
@ApplicationScoped
public class OIDCTokenValidator {
    private static final Logger LOG = Logger.getLogger(OIDCTokenValidator.class);

    private final Instance<OidcSession> oidcSession;

    public OIDCTokenValidator(Instance<OidcSession> oidcSession) {
        this.oidcSession = oidcSession;
    }

    /**
     * Get the current OIDC ID token
     */
    public Optional<String> getIdToken() {
        if (!oidcSession.isResolvable()) {
            return Optional.empty();
        }

        String idToken = oidcSession.get().getIdToken().getRawToken();
        return Optional.ofNullable(idToken);
    }

    /**
     * Validate if OIDC session is active
     */
    public boolean isSessionActive() {
        return oidcSession.isResolvable();
    }

    /**
     * Get the subject from OIDC session
     */
    public Optional<String> getSubject() {
        if (!oidcSession.isResolvable()) {
            return Optional.empty();
        }

        return Optional.ofNullable(oidcSession.get().getIdToken().getSubject());
    }
}
