# 7. Security & JWT

## 7.1 How Spring Security works

Spring Security is a **chain of servlet filters** in front of your controllers (like stacking `app.use(authMiddleware)`). You configure it with a `SecurityFilterChain` bean:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)                                  // stateless bearer-token API
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
            .requestMatchers("/actuator/health/**").permitAll()
            .anyRequest().authenticated())                                     // deny-by-default
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))    // validate "Authorization: Bearer <jwt>"
        .build();
}
```

The authenticated user ends up in the `SecurityContextHolder` (a thread-local) → available as `@AuthenticationPrincipal Jwt jwt` in controllers, or `SecurityContextHolder.getContext().getAuthentication()` anywhere.

401 vs 403: **401** = who are you? (missing/invalid token) · **403** = I know you, but no.

## 7.2 The token flow in this repo

```
1. POST /api/auth/login  (email+password)
      auth-service: AuthenticationManager → DaoAuthenticationProvider → JpaUserDetailsService + PasswordEncoder
      → TokenService signs a JWT with its RSA PRIVATE key (RS256)
2. client calls  GET /api/customers/me   Authorization: Bearer eyJ...
      api-gateway: validates signature/expiry/issuer/audience with the PUBLIC key from auth-service's /.well-known/jwks.json
      customer-service: validates AGAIN (zero trust) and checks roles (@PreAuthorize)
3. application-service → customer-service: Feign interceptor relays the same token (FeignConfig)
```

JWT claims we issue (`TokenService`):

```json
{ "iss": "http://localhost:8081", "aud": ["homefin-api"], "sub": "<user uuid>",
  "iat": 1790000000, "exp": 1790000900, "email": "jane@example.com", "roles": ["CUSTOMER"] }
```

Why RS256 + JWKS instead of an HS256 shared secret (common in Node tutorials)? Only auth-service can *sign*; everyone else only *verifies*; keys can rotate (`kid` header) without redeploying consumers.

Resource-server config (`customer-service/application.yml`):

```yaml
spring.security.oauth2.resourceserver.jwt:
  jwk-set-uri: http://localhost:8081/.well-known/jwks.json
  issuer-uri: http://localhost:8081       # validates "iss"
  audiences: homefin-api                  # validates "aud"
```

Role mapping: the `roles` claim → `ROLE_CUSTOMER` authority via the `JwtAuthenticationConverter` bean from `common-lib`. (Spring's default would read the `scope` claim into `SCOPE_xxx`.)

## 7.3 Authorization rules

- URL rules in the filter chain (coarse).
- **Method security** (fine-grained), enabled by `@EnableMethodSecurity`:

```java
@PreAuthorize("hasRole('ADMIN')")                     // before the method
@PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER')")
@PostAuthorize("hasRole('ADMIN') or returnObject.customerUserId().toString() == authentication.name")  // after (ownership)
```

Ownership checks are critical: *authenticated* ≠ *allowed to see this record* (OWASP API1: Broken Object Level Authorization). For lists, filter by the current user in the query (`findAllByCustomerUserId(userId, ...)`) instead of post-filtering.

## 7.4 Passwords

`PasswordEncoderFactories.createDelegatingPasswordEncoder()` → BCrypt, stored as `{bcrypt}$2a$10$...` so you can migrate algorithms later. Never log passwords (override `toString` on request records), return the same error for unknown user and wrong password (`InvalidCredentialsException`), and rate-limit login in production.

## 7.5 In production: use a real Authorization Server

Our auth-service shows the mechanics, but it's simplified (ephemeral key, no refresh tokens, no revocation, no MFA). Real systems use **Keycloak**, **Spring Authorization Server**, Auth0/Okta, AWS Cognito, Azure Entra ID. Your services stay the same — they're resource servers pointing `issuer-uri`/`jwk-set-uri` at the IdP. Service-to-service calls without a user use the **client-credentials** grant (`spring-boot-starter-oauth2-client`).

## 7.6 Other security essentials

| Topic | Guidance |
|---|---|
| CSRF | Disable only for stateless token APIs; keep it for cookie/session-based web apps |
| CORS | Configure once at the gateway (`spring.cloud.gateway.server.webflux.globalcors`) or `http.cors(...)` — never `*` with credentials |
| Secrets | Env vars / Vault / cloud secret managers — never in git, never in logs |
| Input | Validate everything (chapter 5); use parameterised queries (JPA does); beware `nativeQuery` string concatenation |
| PII | Mask in responses (`nationalIdMasked`) and logs; encrypt sensitive columns at rest |
| Actuator | Expose only health/info publicly; protect or network-isolate `/actuator/prometheus`, `/env`, `/beans` |
| Dependencies | Scan with OWASP Dependency-Check / Snyk / GitHub Dependabot; keep Spring Boot patched |
| Headers | Spring Security sets sane defaults (X-Content-Type-Options, HSTS on HTTPS, etc.) |

## 7.7 Testing security

```java
mvc.perform(get("/api/customers/me")
        .with(jwt().jwt(j -> j.subject(USER_ID.toString()))
                   .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
   .andExpect(status().isOk());
```

`spring-security-test`'s `jwt()` post-processor injects an authenticated token without a real IdP (see `CustomerControllerTest`, `FinanceApplicationFlowIT`). `@WithMockUser` works for non-JWT setups.

## Sources
- Spring Security architecture: <https://docs.spring.io/spring-security/reference/servlet/architecture.html>
- OAuth2 Resource Server – JWT: <https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html>
- Method security: <https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html>
- Password storage: <https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html>
- Spring Authorization Server: <https://docs.spring.io/spring-authorization-server/reference/>
- OWASP API Security Top 10: <https://owasp.org/API-Security/>
- OWASP JWT cheat sheet: <https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html>
