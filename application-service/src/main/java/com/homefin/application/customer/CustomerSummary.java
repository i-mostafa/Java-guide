package com.homefin.application.customer;

import java.util.UUID;

/**
 * Our OWN view of a customer - only the fields this service needs.
 * Don't share DTO classes between services: it couples their release cycles.
 */
public record CustomerSummary(UUID id, UUID userId, String firstName, String lastName, String kycStatus) {

    public boolean isKycVerified() {
        return "VERIFIED".equals(kycStatus);
    }
}
