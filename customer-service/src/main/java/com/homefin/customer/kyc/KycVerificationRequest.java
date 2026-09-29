package com.homefin.customer.kyc;

import java.time.LocalDate;

/** Shape of the 3rd-party provider's API - kept separate from our own DTOs (anti-corruption layer). */
public record KycVerificationRequest(String firstName, String lastName, LocalDate dateOfBirth, String nationalId) {

    @Override
    public String toString() {
        return "KycVerificationRequest[firstName=%s]".formatted(firstName);
    }
}
