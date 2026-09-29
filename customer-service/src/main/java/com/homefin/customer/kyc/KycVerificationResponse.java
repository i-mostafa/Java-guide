package com.homefin.customer.kyc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Tolerant reader: ignore fields we don't know so provider additions don't break us.
 *
 * <p>The provider's JSON response, parsed by Jackson (Java's JSON library, the {@code JSON.parse} + class mapping
 * layer) into this immutable {@code record}. Only the fields we care about are declared.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} (runtime, read by Jackson while parsing): extra JSON fields
 * are silently dropped instead of causing an error. Like a zod schema with {@code .passthrough()}/non-strict mode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
// Integer (capital I) is the object "wrapper" of the primitive int, so it can be null when the field is missing.
public record KycVerificationResponse(String referenceId, String status, Integer score) {

    // A normal instance method on a record. "boolean" is the primitive true/false type.
    public boolean approved() {
        // Constant first ("APPROVED".equals...) is a null-safe idiom: no NullPointerException if status is null.
        return "APPROVED".equalsIgnoreCase(status);
    }
}
