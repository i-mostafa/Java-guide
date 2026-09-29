package com.homefin.application.finance.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.homefin.application.finance.ApplicationStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(@NotNull ApplicationStatus status, @Size(max = 500) String reason) {

    /** Quick cross-field rule without writing a custom validator. */
    @JsonIgnore
    @AssertTrue(message = "reason is required when rejecting an application")
    public boolean isReasonPresentWhenRejected() {
        return status != ApplicationStatus.REJECTED || (reason != null && !reason.isBlank());
    }
}
