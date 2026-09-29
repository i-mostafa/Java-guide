package com.homefin.common.security;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Maps the JWT claim  "roles": ["CUSTOMER"]  to Spring authorities  ROLE_CUSTOMER,
 * so that  {@code @PreAuthorize("hasRole('CUSTOMER')")}  works.
 *
 * <p>Spring Security represents permissions as "granted authorities" (strings). hasRole('X')
 * checks for the authority "ROLE_X", so we add that prefix. TS analogy: a small function in a
 * Passport JWT strategy's verify callback that maps {@code payload.roles} onto {@code req.user}.
 * Used by {@code CommonWebAutoConfiguration}, which exposes the converter as a Spring bean.
 */
// A "utility class": final + private constructor + only static members = a module of functions.
public final class JwtRolesConverterFactory {

    public static final String ROLES_CLAIM = "roles";

    private JwtRolesConverterFactory() {
    }

    // A static factory method, called as JwtRolesConverterFactory.create() - no instance needed.
    public static JwtAuthenticationConverter create() {
        // "new" creates an object instance, exactly as in TS.
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLES_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
