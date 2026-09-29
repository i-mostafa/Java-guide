package com.homefin.auth.auth;

import com.homefin.auth.config.JwtKeyConfig;
import com.homefin.auth.config.JwtProperties;
import com.homefin.auth.user.Role;
import com.homefin.auth.user.User;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

// Static import of AssertJ's entry point: assertThat(x).isEqualTo(y) ~ jest's expect(x).toBe(y).
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test: no Spring context, runs in milliseconds.
 *
 * <p>All collaborators are created by hand with "new" (the config classes are plain Java objects,
 * so their {@code @Bean} methods can be called directly). Like a jest test that instantiates a
 * NestJS service manually instead of using {@code Test.createTestingModule}.
 */
class TokenServiceTest {

    // @Test (JUnit 5): a test case. JUnit creates a fresh instance of the class for every test method.
    @Test
    void issuedTokenIsVerifiableAndCarriesRoles() throws Exception {
        // Arrange
        var keyConfig = new JwtKeyConfig();
        RSAKey key = keyConfig.rsaKey();
        var props = new JwtProperties("http://issuer", "homefin-api", Duration.ofMinutes(5));
        // A frozen clock, like jest.useFakeTimers().setSystemTime(...).
        Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        var service = new TokenService(keyConfig.jwtEncoder(key), key, props, clock);

        User user = User.create("a@b.com", "hash", "A", "B", Role.CUSTOMER);
        // The id is normally assigned by Hibernate on save and has no setter, so a test helper sets the
        // private field via reflection.
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID()); // simulate a persisted entity

        // Act: sign a token, then verify it with the same decoder the app uses.
        String token = service.issueAccessToken(user);
        Jwt jwt = keyConfig.jwtDecoder(key, props).decode(token);

        // Assert (AssertJ fluent assertions).
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        // containsExactly = same elements in the same order, like expect(arr).toEqual(['CUSTOMER']).
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
        assertThat(jwt.getAudience()).containsExactly("homefin-api");
    }
}
