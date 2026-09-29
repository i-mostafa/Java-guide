package com.homefin.application.finance;

import com.homefin.application.config.FinanceProperties;
import com.homefin.application.customer.CustomerGateway;
import com.homefin.application.customer.CustomerSummary;
import com.homefin.application.finance.dto.CreateApplicationRequest;
import com.homefin.application.valuation.ValuationGateway;
import com.homefin.application.valuation.ValuationResponse;
import com.homefin.common.error.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FinanceApplicationServiceTest {

    @Mock FinanceApplicationRepository repository;
    @Mock CustomerGateway customerGateway;
    @Mock ValuationGateway valuationGateway;
    @Mock ApplicationEventPublisher events;
    @Mock TransactionTemplate tx;

    FinanceApplicationService service;

    @BeforeEach
    void setUp() {
        service = new FinanceApplicationService(repository, customerGateway, valuationGateway,
                new InstallmentCalculator(), new FinanceProperties(new BigDecimal("0.0499"), new BigDecimal("0.80")),
                events, tx);
    }

    @Test
    void rejectsWhenKycNotVerified() {
        given(customerGateway.currentCustomer())
                .willReturn(new CustomerSummary(UUID.randomUUID(), UUID.randomUUID(), "A", "B", "PENDING"));

        assertThatThrownBy(() -> service.submit(UUID.randomUUID(), request("1000000")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("KYC");
        then(valuationGateway).should(never()).estimate(any());
    }

    @Test
    void rejectsWhenValuationMakesFtvTooHigh() {
        given(customerGateway.currentCustomer())
                .willReturn(new CustomerSummary(UUID.randomUUID(), UUID.randomUUID(), "A", "B", "VERIFIED"));
        // Declared 1.5M but the valuer says 1.1M -> 1.0M / 1.1M = 91% > 80%
        given(valuationGateway.estimate(any()))
                .willReturn(new ValuationResponse("V-1", "DXB-1", new BigDecimal("1100000"), "AED", "HIGH"));

        assertThatThrownBy(() -> service.submit(UUID.randomUUID(), request("1000000")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Finance-to-value");
        then(tx).should(never()).execute(any());
    }

    private static CreateApplicationRequest request(String finance) {
        return new CreateApplicationRequest("DXB-1", "Dubai", PropertyType.VILLA,
                new BigDecimal("1500000"), new BigDecimal(finance), 300);
    }
}
