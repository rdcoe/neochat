package com.neochat.common.security.service;

import com.neochat.common.security.model.IdentityTokenClaims;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test-only TokenValidator replacement.
 *
 * Mock token format:
 * mock:{subject}:{email}:{role1,role2}:{group1,group2}
 */
@Alternative
@Priority(1)
@Dependent
public class MockPersonaTokenValidator extends TokenValidator {

    public MockPersonaTokenValidator(JWTParser jwtParser, Instance<JwtVerificationKeySource> verificationKeySource) {
        super(jwtParser, verificationKeySource);
    }

    @Override
    public JsonWebToken validateToken(String token) throws ParseException {
        return MockJsonWebToken.parse(token);
    }

    @Override
    public IdentityTokenClaims extractIdentityClaims(String token) {
        MockJsonWebToken jwt;
        try {
            jwt = MockJsonWebToken.parse(token);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        @SuppressWarnings("unchecked")
        Set<String> groups = (Set<String>) jwt.getClaim("groups");

        return new IdentityTokenClaims(
                jwt.getSubject(),
                jwt.getClaim("email"),
                jwt.getGroups(),
                groups);
    }

    public static String buildToken(String subject, String email, Set<String> roles, Set<String> groups) {
        return String.format(
                "mock:%s:%s:%s:%s",
                subject,
                email,
                String.join(",", roles),
                String.join(",", groups));
    }

    private static final class MockJsonWebToken implements JsonWebToken {
        private final String rawToken;
        private final String subject;
        private final String email;
        private final Set<String> roles;
        private final Set<String> groups;

        private MockJsonWebToken(String rawToken, String subject, String email, Set<String> roles, Set<String> groups) {
            this.rawToken = rawToken;
            this.subject = subject;
            this.email = email;
            this.roles = roles;
            this.groups = groups;
        }

        static MockJsonWebToken parse(String token) throws ParseException {
            if (token == null || token.isBlank()) {
                throw new ParseException("Token is empty");
            }

            String[] parts = token.split(":", -1);
            if (parts.length != 5 || !"mock".equals(parts[0])) {
                throw new ParseException("Invalid mock token format");
            }

            String subject = parts[1];
            String email = parts[2];
            Set<String> roles = parseCsv(parts[3]);
            Set<String> groups = parseCsv(parts[4]);

            if (subject.isBlank()) {
                throw new ParseException("Subject cannot be empty");
            }

            return new MockJsonWebToken(token, subject, email, roles, groups);
        }

        private static Set<String> parseCsv(String value) {
            if (value == null || value.isBlank()) {
                return Set.of();
            }
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .collect(Collectors.toSet());
        }

        @Override
        public String getName() {
            return subject;
        }

        @Override
        public String getRawToken() {
            return rawToken;
        }

        @Override
        public String getSubject() {
            return subject;
        }

        @Override
        public Set<String> getGroups() {
            return roles;
        }

        @Override
        public Set<String> getClaimNames() {
            return Set.of("sub", "email", "groups");
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getClaim(String claimName) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", subject);
            claims.put("email", email);
            claims.put("groups", groups);
            claims.put("raw_token", rawToken);
            return (T) claims.get(claimName);
        }
    }
}
