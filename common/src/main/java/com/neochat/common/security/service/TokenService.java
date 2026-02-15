package com.neochat.common.security.service;

import com.neochat.common.security.vault.VaultKeyProvider;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Set;

/**
 * Service for signing and managing identity tokens
 */
@ApplicationScoped
public class TokenService {
    private static final Logger LOG = Logger.getLogger(TokenService.class);

    @Inject
    VaultKeyProvider vaultKeyProvider;

    /**
     * Sign an identity token with the given claims
     * Minimal payload: sub, email, roles, groups
     */
    public String signIdentityToken(String subject, String email, Set<String> roles, Set<String> groups) {
        try {
            var privateKey = vaultKeyProvider.getPrivateKey();
            
            var builder = Jwt.subject(subject)
                    .claim("email", email);

            if (roles != null && !roles.isEmpty()) {
                builder.groups(roles);
            }

            if (groups != null && !groups.isEmpty()) {
                builder.claim("groups", groups);
            }

            String token = builder.sign(privateKey);
            LOG.debugf("Generated identity token for subject: %s", subject);
            return token;
        } catch (Exception e) {
            LOG.error("Failed to sign identity token", e);
            throw new RuntimeException("Failed to sign identity token", e);
        }
    }

    /**
     * Sign a JWT access token with scopes
     */
    public String signAccessToken(String subject, String issuer, String audience, Set<String> scopes, long expirationSeconds) {
        try {
            var privateKey = vaultKeyProvider.getPrivateKey();
            
            var builder = Jwt.subject(subject)
                    .issuer(issuer)
                    .audience(audience)
                    .expiresIn(expirationSeconds);

            if (scopes != null && !scopes.isEmpty()) {
                builder.claim("scope", String.join(" ", scopes));
            }

            String token = builder.sign(privateKey);
            LOG.debugf("Generated access token for subject: %s", subject);
            return token;
        } catch (Exception e) {
            LOG.error("Failed to sign access token", e);
            throw new RuntimeException("Failed to sign access token", e);
        }
    }
}
