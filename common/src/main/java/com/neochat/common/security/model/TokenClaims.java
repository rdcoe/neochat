package com.neochat.common.security.model;

import java.util.Set;

/**
 * Base interface for token claims
 */
public interface TokenClaims {
    String getSubject();
    String getEmail();
    Set<String> getRoles();
    Set<String> getGroups();
}
