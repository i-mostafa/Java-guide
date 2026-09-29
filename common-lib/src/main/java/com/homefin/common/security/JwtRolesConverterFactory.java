package com.homefin.common.security;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Maps the JWT claim  "roles": ["CUSTOMER"]  to Spring authorities  ROLE_CUSTOMER,
 * so that  @PreAuthorize("hasRole('CUSTOMER')")  works.
 */
public final class JwtRolesConverterFactory {

    public static final String ROLES_CLAIM = "roles";

    private JwtRolesConverterFactory() {
    }

    public static JwtAuthenticationConverter create() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLES_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
