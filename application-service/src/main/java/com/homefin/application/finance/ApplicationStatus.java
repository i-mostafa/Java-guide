package com.homefin.application.finance;

import java.util.Map;
import java.util.Set;

/** A tiny state machine: which status can move to which. */
public enum ApplicationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED;

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = Map.of(
            SUBMITTED, Set.of(UNDER_REVIEW, REJECTED),
            UNDER_REVIEW, Set.of(APPROVED, REJECTED),
            APPROVED, Set.of(),
            REJECTED, Set.of());

    public boolean canTransitionTo(ApplicationStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}
