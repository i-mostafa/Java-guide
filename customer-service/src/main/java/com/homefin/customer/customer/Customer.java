package com.homefin.customer.customer;

import com.homefin.common.error.BusinessRuleException;
import com.homefin.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * "Rich" entity: state changes go through intention-revealing methods (updateProfile, markKycResult)
 * instead of public setters, so invariants live in one place.
 */
@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseEntity {

    /** The auth-service user id ("sub" claim). Services share IDs, never database tables. */
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Sensitive PII: in production encrypt at rest (column-level encryption / KMS). */
    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "kyc_reference", length = 64)
    private String kycReference;

    @Column(name = "kyc_checked_at")
    private Instant kycCheckedAt;

    public static Customer register(UUID userId, String email, String firstName, String lastName) {
        Customer c = new Customer();
        c.userId = userId;
        c.email = email;
        c.firstName = firstName;
        c.lastName = lastName;
        return c;
    }

    public void updateProfile(String firstName, String lastName, String phoneNumber,
                              LocalDate dateOfBirth, String nationalId) {
        if (kycStatus == KycStatus.VERIFIED && !nationalId.equals(this.nationalId)) {
            throw new BusinessRuleException("national-id-locked", "National ID cannot change after KYC verification");
        }
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.nationalId = nationalId;
    }

    public boolean isProfileComplete() {
        return dateOfBirth != null && nationalId != null && phoneNumber != null;
    }

    public void markKycResult(boolean approved, String reference, Instant checkedAt) {
        this.kycStatus = approved ? KycStatus.VERIFIED : KycStatus.REJECTED;
        this.kycReference = reference;
        this.kycCheckedAt = checkedAt;
    }
}
