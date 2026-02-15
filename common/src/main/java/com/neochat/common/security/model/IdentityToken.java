package com.neochat.common.security.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Identity token with user claims
 */
public class IdentityToken {
    @JsonProperty("identity_token")
    private String token;
    
    private String subject;
    private String email;
    private Set<String> roles;
    private Set<String> groups;

    public IdentityToken() {
    }

    public IdentityToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }
}
