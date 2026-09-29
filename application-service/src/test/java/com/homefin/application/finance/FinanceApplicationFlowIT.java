package com.homefin.application.finance;

import com.homefin.application.customer.CustomerGateway;
import com.homefin.application.customer.CustomerSummary;
import com.homefin.application.support.TestcontainersConfig;
import com.homefin.application.valuation.ValuationGateway;
import com.homefin.application.valuation.ValuationResponse;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

// Static imports: the MockMvc/Mockito DSL functions (post, get, status, jsonPath, given, any...) can be used
// without their class names, which makes the tests read like a supertest chain.
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Whole service running against real Postgres + Kafka (Testcontainers).
 * Remote dependencies (customer-service, valuation provider) are replaced by Mockito beans.
 *
 * <p>Role: an integration test ("IT" suffix: typically run by Maven Failsafe in the verify phase). It boots the
 * real Spring context (security, validation, JPA, Flyway, Kafka producer) and drives it through HTTP with MockMvc.
 * TS analogy: a jest + supertest test against {@code app} created from the real Nest module, with
 * {@code jest.mock} for the two outbound HTTP gateways and testcontainers-node for Postgres/Kafka.
 *
 * <p>MockMvc calls the Spring MVC stack in-process (no real network socket), including the security filters,
 * so {@code .with(jwt())} can inject a fake authenticated JWT without an auth server.
 */
// @SpringBootTest (runtime, Spring test framework): start the full application context once for this test class
// (and cache it for other tests with the same configuration).
@SpringBootTest
// @AutoConfigureMockMvc: create a MockMvc bean wired to the application (like supertest's request(app)).
@AutoConfigureMockMvc
// @ActiveProfiles("test"): also load application-test.yml (disables config server, Eureka, tracing).
@ActiveProfiles("test")
// @Import: add TestcontainersConfig's beans (the Postgres and Kafka containers) to the context.
@Import(TestcontainersConfig.class)
class FinanceApplicationFlowIT {

    // static final = one constant shared by all tests of this class.
    private static final UUID USER_ID = UUID.randomUUID();

    // @Autowired (runtime): field injection; Spring sets this field from the context. Fine in tests
    // (production code prefers constructor injection).
    @Autowired
    MockMvc mvc;

    // @MockitoBean (runtime, Spring Framework 6.2+): REPLACE the real bean of this type in the context with a
    // Mockito mock, so the service talks to the mock instead of making HTTP calls. Like jest.mock('./customerGateway').
    @MockitoBean
    CustomerGateway customerGateway;

    @MockitoBean
    ValuationGateway valuationGateway;

    // "throws Exception": mvc.perform declares a checked Exception; the test simply lets it propagate
    // (any exception fails the test).
    @Test
    void submitReadAndReviewApplication() throws Exception {
        // BDD-style stubbing: given(mock.call()).willReturn(value) = jest's mockFn.mockReturnValue(value).
        given(customerGateway.currentCustomer())
                .willReturn(new CustomerSummary(UUID.randomUUID(), USER_ID, "Jane", "Doe", "VERIFIED"));
        // any() = argument matcher "whatever is passed", like expect.anything().
        given(valuationGateway.estimate(any()))
                .willReturn(new ValuationResponse("VAL-1", "DXB-MARINA-1204", new BigDecimal("1450000.00"),
                        "AED", "HIGH"));

        // perform(request) + andExpect(...) chain = supertest's request(app).post(...).send(...).expect(201).
        // .with(customer()) attaches a fake JWT with ROLE_CUSTOMER (helper at the bottom).
        String created = mvc.perform(post("/api/applications").with(customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        // Text block: a multi-line string literal delimited by three double quotes (like a JS
                        // template literal without interpolation). Common indentation is stripped automatically.
                        .content("""
                                {"propertyReference":"DXB-MARINA-1204","city":"Dubai","propertyType":"APARTMENT",
                                 "propertyValue":1500000.00,"financeAmount":1000000.00,"tenureMonths":300}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                // jsonPath("$.status") selects a field of the JSON response body (JSONPath syntax, "$" = root).
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.valuationAmount").value(1450000.00))
                .andReturn().getResponse().getContentAsString();
        // Extract the generated id from the JSON string to use in the next requests.
        String id = JsonPath.read(created, "$.id");

        // the owner can read it, another customer cannot
        // "{id}" in the URL template is filled with the next argument (like a path param).
        mvc.perform(get("/api/applications/{id}", id).with(customer())).andExpect(status().isOk());
        // A different subject with ROLE_CUSTOMER -> @PostAuthorize ownership check fails -> 403.
        mvc.perform(get("/api/applications/{id}", id)
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());

        // admin: valid transition, then an invalid one (SUBMITTED -> ... -> APPROVED -> UNDER_REVIEW)
        // \" is an escaped double quote inside a normal Java string literal.
        mvc.perform(patch("/api/applications/{id}/status", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/applications/{id}/status", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());
        // APPROVED -> UNDER_REVIEW is not allowed by the state machine -> BusinessRuleException -> 422.
        mvc.perform(patch("/api/applications/{id}/status", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("invalid-status-transition"));
    }

    @Test
    void rejectingRequiresReason() throws Exception {
        // REJECTED without a reason fails @AssertTrue in UpdateStatusRequest -> 400 before the service runs,
        // which is why a random (non-existent) id is fine here.
        mvc.perform(patch("/api/applications/{id}/status", UUID.randomUUID()).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("validation-failed"));
    }

    // Helpers returning a request post-processor that makes the request look authenticated with a JWT.
    // The long return type is a nested class written with its fully qualified name instead of an import.
    // jwt().jwt(j -> j.subject(...)) customizes the claims; .authorities(...) sets the granted roles.
    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customer() {
        return jwt().jwt(j -> j.subject(USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
