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
 */
@WebMvcTest(CustomerController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
@ImportAutoConfiguration(CommonWebAutoConfiguration.class)
class CustomerControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CustomerService customerService;

    @MockitoBean
    CustomerMapper mapper;

    @MockitoBean
    JwtDecoder jwtDecoder; // never called: jwt() below injects an authenticated token directly

    @Test
    void meReturnsProfileForCustomer() throws Exception {
        Customer customer = Customer.register(USER_ID, "jane@example.com", "Jane", "Doe");
        given(customerService.getByUserId(USER_ID)).willReturn(customer);
        given(mapper.toResponse(customer)).willReturn(new CustomerResponse(UUID.randomUUID(), USER_ID,
                "jane@example.com", "Jane", "Doe", null, null, null, KycStatus.PENDING, null, Instant.now()));

        mvc.perform(get("/api/customers/me").with(customerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));
    }

    @Test
    void meReturns404ProblemWhenProfileMissing() throws Exception {
        given(customerService.getByUserId(any())).willThrow(new NotFoundException("Customer for user", USER_ID));

        mvc.perform(get("/api/customers/me").with(customerJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("resource-not-found"));
    }

    @Test
    void updateValidatesBody() throws Exception {
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
        mvc.perform(get("/api/customers").with(customerJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedGets401() throws Exception {
        mvc.perform(get("/api/customers/me")).andExpect(status().isUnauthorized());
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt() {
        return jwt().jwt(j -> j.subject(USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}
