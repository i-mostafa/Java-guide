package com.homefin.customer.customer.dto;

import com.homefin.customer.customer.KycStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate dateOfBirth,
        String nationalIdMasked,
        KycStatus kycStatus,
        Instant kycCheckedAt,
        Instant createdAt) {
}
