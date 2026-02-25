package com.neochat.common.security.service;

import com.neochat.common.security.vault.VaultKeyProvider;
import jakarta.ws.rs.InternalServerErrorException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {

    @Test
    void signIdentityToken_whenKeyProviderFails_throwsInternalServerErrorException() {
        VaultKeyProvider failingKeyProvider = new VaultKeyProvider(null) {
            @Override
            public java.security.PrivateKey getPrivateKey() {
                throw new RuntimeException("key retrieval failed");
            }
        };

        TokenService tokenService = new TokenService(failingKeyProvider);

        assertThrows(
                InternalServerErrorException.class,
                () -> tokenService.signIdentityToken("user-1", "user@example.com", Set.of("user"), Set.of("group-1"))
        );
    }

    @Test
    void signAccessToken_whenKeyProviderFails_throwsInternalServerErrorException() {
        VaultKeyProvider failingKeyProvider = new VaultKeyProvider(null) {
            @Override
            public java.security.PrivateKey getPrivateKey() {
                throw new RuntimeException("key retrieval failed");
            }
        };

        TokenService tokenService = new TokenService(failingKeyProvider);

        assertThrows(
                InternalServerErrorException.class,
                () -> tokenService.signAccessToken("user-1", "neochat", "chat-service", Set.of("chat:read"), 3600)
        );
    }
}
