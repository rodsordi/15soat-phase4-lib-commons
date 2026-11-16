package br.com.fiap.phase4.commons.security.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KeycloakJwtAuthenticationConverter Unit Tests")
class KeycloakJwtAuthenticationConverterTest {

    private KeycloakJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakJwtAuthenticationConverter();
    }

    private Jwt createMockJwt(Map<String, Object> claims) {
        return new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256", "typ", "JWT"),
                claims
        );
    }

    @DisplayName("When converting JWT to Authentication Token")
    @Nested
    class Convert {

        @DisplayName("Given a JWT with realm_access roles and preferred_username")
        @Test
        void shouldExtractRolesAndPreferredUsername() {
            var claims = Map.<String, Object>of(
                    "preferred_username", "12345678909",
                    "sub", "user-uuid-12345",
                    "email", "rodrigo.test@example.com",
                    "realm_access", Map.of("roles", List.of("CUSTOMER", "ADMIN"))
            );
            var jwt = createMockJwt(claims);

            AbstractAuthenticationToken token = converter.convert(jwt);

            assertThat(token).isNotNull();
            assertThat(token.getName()).isEqualTo("12345678909");
            assertThat(token.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
        }

        @DisplayName("Given a JWT without preferred_username, should fallback to sub")
        @Test
        void shouldFallbackToSubWhenPreferredUsernameIsNull() {
            var claims = Map.<String, Object>of(
                    "sub", "user-uuid-12345",
                    "realm_access", Map.of("roles", List.of("EMPLOYEE"))
            );
            var jwt = createMockJwt(claims);

            AbstractAuthenticationToken token = converter.convert(jwt);

            assertThat(token).isNotNull();
            assertThat(token.getName()).isEqualTo("user-uuid-12345");
            assertThat(token.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_EMPLOYEE");
        }

        @DisplayName("Given a JWT without realm_access claim, should return empty authorities")
        @Test
        void shouldReturnEmptyAuthoritiesWhenNoRealmAccess() {
            var claims = Map.<String, Object>of(
                    "preferred_username", "12345678909",
                    "sub", "user-uuid-12345"
            );
            var jwt = createMockJwt(claims);

            AbstractAuthenticationToken token = converter.convert(jwt);

            assertThat(token).isNotNull();
            assertThat(token.getAuthorities()).isEmpty();
        }
    }
}
