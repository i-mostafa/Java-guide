package com.homefin.application.customer;

import java.util.UUID;

/**
 * Our OWN view of a customer - only the fields this service needs.
 * Don't share DTO classes between services: it couples their release cycles.
 *
 * <p>Role: the JSON response of customer-service's {@code GET /api/customers/me}, deserialized by Feign
 * (using Jackson, the JSON library). Unknown JSON fields are ignored by Spring Boot's default Jackson
 * settings, so customer-service can add fields without breaking us.
 * TS analogy: {@code type CustomerSummary = { id: string; userId: string; ...; kycStatus: string }}.
 */
// record: immutable data carrier. Fields are declared in the header; Java generates the constructor,
// accessors id(), userId(), ..., equals/hashCode/toString. Jackson can serialize and deserialize records.
// UUID is java.util.UUID, a real type (not a string) that is parsed/validated on deserialization.
public record CustomerSummary(UUID id, UUID userId, String firstName, String lastName, String kycStatus) {

    // Records can have extra methods. Written as "VERIFIED".equals(x) rather than x.equals("VERIFIED")
    // so it is null-safe: if kycStatus is null it simply returns false instead of throwing
    // a NullPointerException. (Never compare Strings with == in Java: that compares object identity.)
    public boolean isKycVerified() {
        return "VERIFIED".equals(kycStatus);
    }
}
