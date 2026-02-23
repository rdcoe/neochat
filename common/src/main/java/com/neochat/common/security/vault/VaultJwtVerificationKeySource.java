package com.neochat.common.security.vault;

import com.neochat.common.security.service.JwtVerificationKeySource;
import jakarta.enterprise.context.ApplicationScoped;

import java.security.PublicKey;

/**
 * Vault-backed verification key source for JWT signature validation.
 */
@ApplicationScoped
public class VaultJwtVerificationKeySource implements JwtVerificationKeySource {

    private final VaultKeyProvider vaultKeyProvider;

    public VaultJwtVerificationKeySource(VaultKeyProvider vaultKeyProvider) {
        this.vaultKeyProvider = vaultKeyProvider;
    }

    @Override
    public PublicKey getVerificationKey() {
        return vaultKeyProvider.getPublicKey();
    }
}
