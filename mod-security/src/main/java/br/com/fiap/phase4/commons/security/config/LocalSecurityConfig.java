package br.com.fiap.phase4.commons.security.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Local and test security configuration providing a fallback JwtDecoder.
 * Activates under 'local' and 'test' profiles to decode tokens without requiring an external Keycloak instance.
 */
@AutoConfiguration
@Profile({"local", "test"})
public class LocalSecurityConfig {

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "local-e2e-subject")
                .claim("preferred_username", "52998224725")
                .claim("email", "admin@garage.com")
                .claim("realm_access", Map.of("roles", List.of("EMPLOYEE", "ADMIN", "CUSTOMER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
    }
}
