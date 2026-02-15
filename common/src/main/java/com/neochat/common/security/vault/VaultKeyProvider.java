package com.neochat.common.security.vault;

import io.quarkus.vault.VaultKVSecretEngine;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

/**
 * Provides RSA key pairs from various sources (filesystem, HashiCorp Vault, Azure Key Vault)
 * Keys are cached in memory for performance
 */
@ApplicationScoped
public class VaultKeyProvider {
    private static final Logger LOG = Logger.getLogger(VaultKeyProvider.class);

    @ConfigProperty(name = "vault.provider", defaultValue = "filesystem")
    String vaultProvider;

    @ConfigProperty(name = "vault.filesystem.private-key-path", defaultValue = "/keys/private.pem")
    String privateKeyPath;

    @ConfigProperty(name = "vault.filesystem.public-key-path", defaultValue = "/keys/public.pem")
    String publicKeyPath;

    @ConfigProperty(name = "vault.hashicorp.secret-path", defaultValue = "secret/neochat/keys")
    String hashicorpSecretPath;

    @Inject
    @Identifier("default")
    Optional<VaultKVSecretEngine> vaultKVSecretEngine;

    @Inject
    Optional<KeyVaultClient> keyVaultClient;

    private PrivateKey cachedPrivateKey;
    private PublicKey cachedPublicKey;

    /**
     * Get private key for signing
     */
    public PrivateKey getPrivateKey() {
        if (cachedPrivateKey == null) {
            cachedPrivateKey = loadPrivateKey();
        }
        return cachedPrivateKey;
    }

    /**
     * Get public key for verification
     */
    public PublicKey getPublicKey() {
        if (cachedPublicKey == null) {
            cachedPublicKey = loadPublicKey();
        }
        return cachedPublicKey;
    }

    private PrivateKey loadPrivateKey() {
        try {
            String keyContent = switch (vaultProvider.toLowerCase()) {
                case "filesystem" -> loadFromFilesystem(privateKeyPath);
                case "hashicorp" -> loadFromHashiCorpVault("private_key");
                case "azure" -> loadFromAzureKeyVault("private-key");
                default -> throw new IllegalStateException("Unknown vault provider: " + vaultProvider);
            };

            // Remove PEM headers and decode
            String privateKeyPEM = keyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            LOG.error("Failed to load private key", e);
            throw new RuntimeException("Failed to load private key", e);
        }
    }

    private PublicKey loadPublicKey() {
        try {
            String keyContent = switch (vaultProvider.toLowerCase()) {
                case "filesystem" -> loadFromFilesystem(publicKeyPath);
                case "hashicorp" -> loadFromHashiCorpVault("public_key");
                case "azure" -> loadFromAzureKeyVault("public-key");
                default -> throw new IllegalStateException("Unknown vault provider: " + vaultProvider);
            };

            // Remove PEM headers and decode
            String publicKeyPEM = keyContent
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            LOG.error("Failed to load public key", e);
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    private String loadFromFilesystem(String path) throws IOException {
        LOG.infof("Loading key from filesystem: %s", path);
        return Files.readString(Paths.get(path));
    }

    private String loadFromHashiCorpVault(String keyName) {
        LOG.infof("Loading key from HashiCorp Vault: %s/%s", hashicorpSecretPath, keyName);
        if (vaultKVSecretEngine.isEmpty()) {
            throw new IllegalStateException("HashiCorp Vault not configured");
        }
        var secret = vaultKVSecretEngine.get().readSecret(hashicorpSecretPath);
        return (String) secret.get(keyName);
    }

    private String loadFromAzureKeyVault(String keyName) {
        LOG.infof("Loading key from Azure Key Vault: %s", keyName);
        if (keyVaultClient.isEmpty()) {
            throw new IllegalStateException("Azure Key Vault not configured");
        }
        return keyVaultClient.get().getSecret(keyName);
    }

    /**
     * Clear cached keys (useful for testing or key rotation)
     */
    public void clearCache() {
        cachedPrivateKey = null;
        cachedPublicKey = null;
    }
}
