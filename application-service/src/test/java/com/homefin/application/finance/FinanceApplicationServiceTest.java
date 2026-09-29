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

// Static imports of assertion and Mockito DSL functions (called without their class name).
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Pure unit test of FinanceApplicationService's business rules: no Spring, no DB, no HTTP.
 *
 * <p>Role: every collaborator is a Mockito mock and the service is built with {@code new}, calling the
 * Lombok-generated constructor directly - one benefit of constructor injection. TS analogy: jest with
 * {@code jest.fn()} mocks passed into {@code new FinanceApplicationService(...)}, no Nest TestingModule.
 */
// @ExtendWith (runtime, JUnit 5): plug an extension into the test lifecycle. MockitoExtension creates the
// @Mock fields before each test and fails the test on unused stubbings ("strict stubs").
@ExtendWith(MockitoExtension.class)
class FinanceApplicationServiceTest {

    // @Mock (runtime, Mockito): create a mock of the field's type (every method returns a default: null/0/false),
    // like jest.fn() for every method of the class.
    @Mock FinanceApplicationRepository repository;
    @Mock CustomerGateway customerGateway;
    @Mock ValuationGateway valuationGateway;
    @Mock ApplicationEventPublisher events;
    @Mock TransactionTemplate tx;

    FinanceApplicationService service;

    // @BeforeEach (runtime, JUnit): run before every @Test, like jest's beforeEach.
    @BeforeEach
    void setUp() {
        // Real (non-mocked) InstallmentCalculator and FinanceProperties: they are simple value logic.
        service = new FinanceApplicationService(repository, customerGateway, valuationGateway,
                new InstallmentCalculator(), new FinanceProperties(new BigDecimal("0.0499"), new BigDecimal("0.80")),
                events, tx);
    }

    @Test
    void rejectsWhenKycNotVerified() {
        given(customerGateway.currentCustomer())
                .willReturn(new CustomerSummary(UUID.randomUUID(), UUID.randomUUID(), "A", "B", "PENDING"));

        // assertThatThrownBy(() -> code) = expect(() => code).toThrow(...): the lambda must throw.
        assertThatThrownBy(() -> service.submit(UUID.randomUUID(), request("1000000")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("KYC");
        // then(mock).should(never()).method(...) = expect(mock.method).not.toHaveBeenCalled().
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
        // Nothing was written: the transaction was never started.
        then(tx).should(never()).execute(any());
    }

    // private static helper shared by the tests (static: doesn't use instance fields).
    private static CreateApplicationRequest request(String finance) {
        return new CreateApplicationRequest("DXB-1", "Dubai", PropertyType.VILLA,
                new BigDecimal("1500000"), new BigDecimal(finance), 300);
    }
}
