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
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class FinanceApplicationFlowIT {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CustomerGateway customerGateway;

    @MockitoBean
    ValuationGateway valuationGateway;

    @Test
    void submitReadAndReviewApplication() throws Exception {
        given(customerGateway.currentCustomer())
                .willReturn(new CustomerSummary(UUID.randomUUID(), USER_ID, "Jane", "Doe", "VERIFIED"));
        given(valuationGateway.estimate(any()))
                .willReturn(new ValuationResponse("VAL-1", "DXB-MARINA-1204", new BigDecimal("1450000.00"),
                        "AED", "HIGH"));

        String created = mvc.perform(post("/api/applications").with(customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"propertyReference":"DXB-MARINA-1204","city":"Dubai","propertyType":"APARTMENT",
                                 "propertyValue":1500000.00,"financeAmount":1000000.00,"tenureMonths":300}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.valuationAmount").value(1450000.00))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        // the owner can read it, another customer cannot
        mvc.perform(get("/api/applications/{id}", id).with(customer())).andExpect(status().isOk());
        mvc.perform(get("/api/applications/{id}", id)
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());

        // admin: valid transition, then an invalid one (SUBMITTED -> ... -> APPROVED -> UNDER_REVIEW)
        mvc.perform(patch("/api/applications/{id}/status", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/applications/{id}/status", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/applications/{id}/status", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("invalid-status-transition"));
    }

    @Test
    void rejectingRequiresReason() throws Exception {
        mvc.perform(patch("/api/applications/{id}/status", UUID.randomUUID()).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("validation-failed"));
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customer() {
        return jwt().jwt(j -> j.subject(USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
