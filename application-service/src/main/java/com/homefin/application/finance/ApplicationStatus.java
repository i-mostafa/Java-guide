package com.homefin.application.finance;

import java.util.Map;
import java.util.Set;

/**
 * A tiny state machine: which status can move to which.
 *
 * <p>Role: lifecycle of a finance application. Stored in the DB as its name (see {@code @Enumerated} on
 * FinanceApplication) and serialized to JSON as the name ("SUBMITTED"...).
 *
 * <p>Java enums are much richer than TS enums: each constant is a singleton OBJECT of this class, and the
 * enum can have fields and methods. TS analogy: {@code type ApplicationStatus = 'SUBMITTED' | ...} plus a
 * {@code canTransitionTo(from, to)} helper, bundled together.
 */
public enum ApplicationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED;

    // private static final = one shared, never-reassigned constant for the whole enum (UPPER_CASE by convention).
    // Map<ApplicationStatus, Set<ApplicationStatus>> = generics: a map from a status to a set of statuses,
    // like Map<Status, Set<Status>> in TS.
    // Map.of / Set.of create IMMUTABLE collections (calling put/add throws), like Object.freeze.
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = Map.of(
            SUBMITTED, Set.of(UNDER_REVIEW, REJECTED),
            UNDER_REVIEW, Set.of(APPROVED, REJECTED),
            APPROVED, Set.of(),
            REJECTED, Set.of());

    // Instance method: `this` is the enum constant it is called on, e.g. SUBMITTED.canTransitionTo(APPROVED).
    public boolean canTransitionTo(ApplicationStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}
