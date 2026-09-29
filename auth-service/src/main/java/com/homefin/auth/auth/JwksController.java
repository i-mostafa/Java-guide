package com.homefin.auth.auth;

// Nimbus JOSE + JWT: the JWT/JWK library Spring Security uses internally (like "jose" on npm).
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publishes the PUBLIC key so other services can verify our tokens (standard JWKS format).
 *
 * <p>Served at GET /.well-known/jwks.json. The gateway and the resource services download this
 * JSON (and cache it) to check JWT signatures. Express analogy:
 * {@code app.get('/.well-known/jwks.json', (req, res) => res.json({ keys: [publicJwk] }))}.
 */
// @RestController: HTTP handler class; return values become JSON.
@RestController
// @RequiredArgsConstructor (Lombok): constructor injection of the RSAKey bean defined in JwtKeyConfig.
@RequiredArgsConstructor
public class JwksController {

    private final RSAKey rsaKey;

    // @GetMapping: handle GET on this exact path (no class-level @RequestMapping prefix here).
    @GetMapping("/.well-known/jwks.json")
    // Map<String, Object> is like Record<string, unknown>; Jackson serializes it as a JSON object.
    public Map<String, Object> jwks() {
        // toPublicJWK() strips the private part - never expose the private key.
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
