package com.homefin.customer.customer;

import com.homefin.common.error.NotFoundException;
import com.homefin.common.web.CommonWebAutoConfiguration;
import com.homefin.customer.config.SecurityConfig;
import com.homefin.customer.customer.dto.CustomerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

// Static imports of helper functions: get(...)/put(...) build fake requests, status()/jsonPath() build
// response matchers, jwt() fakes an authenticated JWT (from spring-security-test).
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web "slice" test: only the MVC layer (controller, JSON, validation, security) is started;
 * the service is a Mockito mock. Fast and focused - like supertest against an express router
 * with the service layer stubbed.
 *
 * <ul>
 *   <li>{@code @WebMvcTest(CustomerController.class)} (runtime): start a partial Spring context with only the web
 *       layer for this controller: no DB, no Kafka, no other services. Also provides a {@code MockMvc} bean.</li>
 *   <li>{@code @ActiveProfiles("test")}: load application-test.yml on top of application.yml.</li>
 *   <li>{@code @Import(SecurityConfig.class)}: slices don't scan every config class, so include our real security
 *       rules explicitly; the test then checks 401/403 behavior for real.</li>
 *   <li>{@code @ImportAutoConfiguration(CommonWebAutoConfiguration.class)}: add common-lib's web auto-config
 *       (global exception handler producing problem JSON, JWT roles converter).</li>
 * </ul>
 */
@WebMvcTest(CustomerController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
@ImportAutoConfiguration(CommonWebAutoConfiguration.class)
class CustomerControllerTest {

    // A constant shared by all tests in this class.
    private static final UUID USER_ID = UUID.randomUUID();

    // @Autowired (runtime): Spring injects the MockMvc bean into this field before each test.
    // MockMvc sends requests through the full Spring MVC stack in memory (no real HTTP port), like supertest(app).
    @Autowired
    MockMvc mvc;

    // @MockitoBean (runtime, Spring test): put a Mockito mock of this type INTO the Spring context, replacing any real
    // bean, so the controller gets the mock injected. Like overriding a provider with useValue: mock in a
    // NestJS Test.createTestingModule(...).overrideProvider(...).
    @MockitoBean
    CustomerService customerService;

    @MockitoBean
    CustomerMapper mapper;

    @MockitoBean
    JwtDecoder jwtDecoder; // never called: jwt() below injects an authenticated token directly

    // "throws Exception": mvc.perform declares a checked exception, and the test just lets it propagate
    // (a thrown exception fails the test, like an unhandled rejection in jest).
    @Test
    void meReturnsProfileForCustomer() throws Exception {
        Customer customer = Customer.register(USER_ID, "jane@example.com", "Jane", "Doe");
        given(customerService.getByUserId(USER_ID)).willReturn(customer);
        given(mapper.toResponse(customer)).willReturn(new CustomerResponse(UUID.randomUUID(), USER_ID,
                "jane@example.com", "Jane", "Doe", null, null, null, KycStatus.PENDING, null, Instant.now()));

        // Like: await request(app).get('/api/customers/me').set('Authorization', ...).expect(200)
        // .with(customerJwt()) attaches the fake authenticated JWT. jsonPath("$.email") selects a field in the
        // JSON response body (JSONPath syntax, "$" = root).
        mvc.perform(get("/api/customers/me").with(customerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));
    }

    @Test
    void meReturns404ProblemWhenProfileMissing() throws Exception {
        // willThrow: make the mocked method throw, like mockImplementation(() => { throw ... }).
        given(customerService.getByUserId(any())).willThrow(new NotFoundException("Customer for user", USER_ID));

        mvc.perform(get("/api/customers/me").with(customerJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("resource-not-found"));
    }

    @Test
    void updateValidatesBody() throws Exception {
        // """ ... """ is a TEXT BLOCK: a multi-line string literal, like a JS template literal without
        // interpolation. The common leading indentation is stripped automatically.
        // The body breaks 4 rules: blank firstName, bad phone format, under 18, too-short nationalId.
        mvc.perform(put("/api/customers/me").with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Doe","phoneNumber":"0501234",
                                 "dateOfBirth":"2015-01-01","nationalId":"x"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    void adminEndpointForbiddenForCustomer() throws Exception {
        // Authenticated, but only ROLE_CUSTOMER: @PreAuthorize("hasRole('ADMIN')") rejects with 403.
        mvc.perform(get("/api/customers").with(customerJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedGets401() throws Exception {
        // No token at all: the security filter chain rejects with 401 before the controller is reached.
        mvc.perform(get("/api/customers/me")).andExpect(status().isUnauthorized());
    }

    // Returns a request post-processor that makes the request look authenticated with a JWT whose "sub" is USER_ID
    // and whose authority is ROLE_CUSTOMER. The long return type is a fully-qualified nested class name
    // (package.Outer.Inner) written out instead of imported.
    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt() {
        // j -> j.subject(...) is a lambda customizing the fake token's claims.
        return jwt().jwt(j -> j.subject(USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}
