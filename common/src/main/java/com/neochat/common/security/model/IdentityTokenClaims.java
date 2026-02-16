package com.neochat.common.security.model;

import java.util.Set;

/**
 * Claims extracted from identity token
 */
public class IdentityTokenClaims implements TokenClaims {
    private final String subject;
    private final String email;
    private final Set<String> roles;
    private final Set<String> groups;

    public IdentityTokenClaims(String subject, String email, Set<String> roles, Set<String> groups) {
        this.subject = subject;
        this.email = email;
        this.roles = roles != null ? Set.copyOf(roles) : Set.of();
        this.groups = groups != null ? Set.copyOf(groups) : Set.of();
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public Set<String> getGroups() {
        return groups;
    }
}
