package com.homefin.auth.auth;

import com.homefin.auth.config.JwtProperties;
import com.homefin.auth.user.User;
import com.homefin.common.security.JwtRolesConverterFactory;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

// java.time.Clock = an injectable source of "now" (so tests can freeze time).
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Creates and signs JWT access tokens (RS256) for authenticated users.
 *
 * <p>Node analogy: a small service wrapping {@code jwt.sign(payload, privateKey, { algorithm: 'RS256',
 * keyid, issuer, audience, expiresIn })}. The key, settings and clock are all injected, which is
 * what makes {@code TokenServiceTest} possible without starting Spring.
 */
// @Service (Spring, runtime): singleton bean. @RequiredArgsConstructor (Lombok): injection constructor.
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder encoder;
    private final RSAKey rsaKey;
    private final JwtProperties props;
    private final Clock clock;

    public String issueAccessToken(User user) {
        Instant now = clock.instant();
        // Builder pattern: chain setters, then build() an immutable object. Very common in Java
        // because there are no object literals like { iss: ..., sub: ... }.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                // List.of(...) creates an immutable list, like a frozen array literal.
                .audience(List.of(props.audience()))
                .subject(user.getId().toString())   // "sub" = stable user id, never the email
                .issuedAt(now)
                // Instant.plus(Duration) = date arithmetic; java.time objects are immutable, so it
                // returns a new Instant instead of mutating "now".
                .expiresAt(now.plus(props.accessTokenTtl()))
                .claim("email", user.getEmail())
                // Role is an enum; name() returns the constant's name as a String ("CUSTOMER").
                .claim(JwtRolesConverterFactory.ROLES_CLAIM, List.of(user.getRole().name()))
                .build();
        // "kid" header tells verifiers which key in the JWKS to use.
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(rsaKey.getKeyID()).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // Returned to clients as "expiresIn" in the login response.
    public long ttlSeconds() {
        return props.accessTokenTtl().toSeconds();
    }
}
