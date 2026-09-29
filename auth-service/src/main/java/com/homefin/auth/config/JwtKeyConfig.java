package com.homefin.auth.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

// java.security = the JDK's built-in crypto API (like Node's "crypto" module).
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

/**
 * Asymmetric (RS256) JWT signing:
 *  - auth-service signs with the PRIVATE key
 *  - every other service verifies with the PUBLIC key fetched from /.well-known/jwks.json
 * So no shared secret has to be distributed (unlike HS256 "jwt secret" setups common in Node).
 *
 * DEV ONLY: a fresh key pair is generated at startup, so tokens become invalid after a restart
 * and multiple instances would each have their own key. In production load the key from a
 * secret manager / KMS / Vault, or use a real Authorization Server (Keycloak, Spring
 * Authorization Server, Auth0, Cognito...).
 *
 * <p>Defines three beans: the key pair, an encoder (used by TokenService to sign) and a decoder
 * (used by Spring Security to verify incoming bearer tokens on this service's own endpoints).
 * Node analogy: {@code crypto.generateKeyPairSync('rsa', ...)} at boot plus {@code jose}'s
 * SignJWT / jwtVerify wrapped as injectable providers.
 */
@Slf4j
// @Configuration: its @Bean methods are called by Spring at startup to create beans.
@Configuration
public class JwtKeyConfig {

    // "throws NoSuchAlgorithmException": this method may throw a CHECKED exception, so it has to be
    // declared (the compiler enforces it). Spring would fail startup if it were thrown.
    @Bean
    public RSAKey rsaKey() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        log.warn("Generated an ephemeral RSA signing key - DO NOT use this in production");
        // "(RSAPublicKey) keyPair.getPublic()" casts the generic PublicKey to the RSA-specific type.
        // RSAKey.Builder is a static nested class, created with "new Outer.Inner(...)".
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                // Random key id ("kid"); lets verifiers pick the right key after a key rotation.
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    // Bean method parameters are injected by Spring: here the RSAKey bean created above.
    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {
        // new ImmutableJWKSet<>(...): the diamond "<>" lets Java infer the generic type argument.
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    /** auth-service also protects its own endpoints (e.g. /api/auth/me) with the same key. */
    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey, JwtProperties props) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        // OAuth2TokenValidator<Jwt> = generic interface "a validator for Jwt tokens".
        // JwtClaimValidator<List<String>> checks one claim ("aud") with a predicate lambda that must
        // return true: aud -> aud != null && aud.contains(...), like (aud) => !!aud?.includes(...).
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, aud -> aud != null && aud.contains(props.audience()));
        // Combine the default checks (expiry, not-before, issuer) with our audience check.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.issuer()), audience));
        return decoder;
    }
}
