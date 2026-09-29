package com.homefin.customer.customer;

import com.homefin.common.error.BusinessRuleException;
import com.homefin.customer.kyc.KycClient;
import com.homefin.customer.kyc.KycVerificationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/** Pure unit test with Mockito (like jest.mock). No Spring, no DB. */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    CustomerRepository customers;

    @Mock
    KycClient kycClient;

    @InjectMocks
    CustomerService service;

    @Test
    void kycRequiresCompleteProfile() {
        UUID userId = UUID.randomUUID();
        given(customers.findByUserId(userId))
                .willReturn(Optional.of(Customer.register(userId, "a@b.com", "A", "B")));

        assertThatThrownBy(() -> service.verifyKyc(userId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Complete");
        then(kycClient).should(never()).verify(any(), anyString());
    }

    @Test
    void approvedKycMarksCustomerVerified() {
        UUID userId = UUID.randomUUID();
        Customer customer = Customer.register(userId, "a@b.com", "A", "B");
        ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());
        customer.updateProfile("A", "B", "+971501234567", LocalDate.of(1990, 1, 1), "784-1990-1234567-1");

        given(customers.findByUserId(userId)).willReturn(Optional.of(customer));
        given(customers.findById(customer.getId())).willReturn(Optional.of(customer));
        given(customers.save(any(Customer.class))).willAnswer(inv -> inv.getArgument(0));
        given(kycClient.verify(any(), anyString()))
                .willReturn(new KycVerificationResponse("KYC-123", "APPROVED", 91));

        Customer result = service.verifyKyc(userId);

        assertThat(result.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(result.getKycReference()).isEqualTo("KYC-123");
    }
}
