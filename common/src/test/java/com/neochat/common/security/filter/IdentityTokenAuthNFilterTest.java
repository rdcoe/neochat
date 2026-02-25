package com.neochat.common.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neochat.common.security.model.IdentityTokenClaims;
import com.neochat.common.security.service.OIDCTokenValidator;
import com.neochat.common.security.service.TokenService;
import com.neochat.common.security.service.TokenValidator;
import com.neochat.common.security.vault.VaultKeyProvider;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityTokenAuthNFilterTest {

    @Test
    void filter_whenTokenSigningFails_propagatesInternalServerErrorException() {
        TokenValidator tokenValidator = new TokenValidator(null, null) {
            @Override
            public IdentityTokenClaims extractIdentityClaims(String token) {
                return new IdentityTokenClaims("user-1", "user@example.com", Set.of("user"), Set.of("group-1"));
            }
        };

        OIDCTokenValidator oidcTokenValidator = new OIDCTokenValidator(null) {
            @Override
            public Optional<String> getIdToken() {
                return Optional.of("oidc-id-token");
            }
        };

        VaultKeyProvider failingKeyProvider = new VaultKeyProvider(null) {
            @Override
            public java.security.PrivateKey getPrivateKey() {
                throw new RuntimeException("key retrieval failed");
            }
        };

        TokenService tokenService = new TokenService(failingKeyProvider);

        IdentityTokenAuthNFilter filter = new IdentityTokenAuthNFilter(
                tokenValidator,
                tokenService,
                oidcTokenValidator,
                new ObjectMapper()
        );

        ContainerRequestContext requestContext = createRequestContext("api/messages");

        assertThrows(InternalServerErrorException.class, () -> filter.filter(requestContext));
    }

    private static ContainerRequestContext createRequestContext(String path) {
        UriInfo uriInfo = (UriInfo) Proxy.newProxyInstance(
                UriInfo.class.getClassLoader(),
                new Class[]{UriInfo.class},
                (proxy, method, args) -> {
                    if ("getPath".equals(method.getName())) {
                        return path;
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        return (ContainerRequestContext) Proxy.newProxyInstance(
                ContainerRequestContext.class.getClassLoader(),
                new Class[]{ContainerRequestContext.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getUriInfo" -> uriInfo;
                        case "hasEntity" -> false;
                        case "getMediaType" -> null;
                        case "getHeaderString" -> null;
                        case "setProperty", "abortWith", "setEntityStream" -> null;
                        default -> defaultValue(method.getReturnType());
                    };
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
