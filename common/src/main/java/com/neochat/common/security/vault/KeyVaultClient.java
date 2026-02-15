package com.neochat.common.security.vault;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Client for Azure Key Vault operations
 */
@ApplicationScoped
public class KeyVaultClient {
    private static final Logger LOG = Logger.getLogger(KeyVaultClient.class);

    @ConfigProperty(name = "vault.azure.key-vault-url")
    Optional<String> keyVaultUrl;

    private KeyClient keyClient;

    public String getSecret(String secretName) {
        if (keyVaultUrl.isEmpty()) {
            throw new IllegalStateException("Azure Key Vault URL not configured");
        }

        if (keyClient == null) {
            LOG.infof("Initializing Azure Key Vault client for: %s", keyVaultUrl.get());
            keyClient = new KeyClientBuilder()
                    .vaultUrl(keyVaultUrl.get())
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
        }

        LOG.infof("Retrieving secret from Azure Key Vault: %s", secretName);
        // Note: In real implementation, you'd use SecretClient for secrets
        // This is simplified for the key use case
        return keyClient.getKey(secretName).getKey().toString();
    }
}
