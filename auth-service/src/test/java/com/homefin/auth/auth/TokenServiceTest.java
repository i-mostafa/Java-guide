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

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test: no Spring context, runs in milliseconds. */
class TokenServiceTest {

    @Test
    void issuedTokenIsVerifiableAndCarriesRoles() throws Exception {
        var keyConfig = new JwtKeyConfig();
        RSAKey key = keyConfig.rsaKey();
        var props = new JwtProperties("http://issuer", "homefin-api", Duration.ofMinutes(5));
        Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        var service = new TokenService(keyConfig.jwtEncoder(key), key, props, clock);

        User user = User.create("a@b.com", "hash", "A", "B", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID()); // simulate a persisted entity

        String token = service.issueAccessToken(user);
        Jwt jwt = keyConfig.jwtDecoder(key, props).decode(token);

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
        assertThat(jwt.getAudience()).containsExactly("homefin-api");
    }
}
