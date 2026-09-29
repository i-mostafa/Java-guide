package com.homefin.customer.kyc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Tolerant reader: ignore fields we don't know so provider additions don't break us. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KycVerificationResponse(String referenceId, String status, Integer score) {

    public boolean approved() {
        return "APPROVED".equalsIgnoreCase(status);
    }
}
