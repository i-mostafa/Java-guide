package com.homefin.application.finance.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.homefin.application.finance.ApplicationStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JSON request body of {@code PATCH /api/applications/{id}/status} (admin only), e.g.
 * {@code {"status":"REJECTED","reason":"Income not verified"}}.
 *
 * <p>Validated at runtime because the controller parameter is annotated {@code @Valid}.
 * TS analogy: {@code z.object({ status: z.enum([...]), reason: z.string().max(500).optional() })
 * .refine(r => r.status !== 'REJECTED' || !!r.reason?.trim())}.
 */
// Constraint annotations can sit directly on record components: @NotNull status, @Size(max = 500) reason.
public record UpdateStatusRequest(@NotNull ApplicationStatus status, @Size(max = 500) String reason) {

    /**
     * Quick cross-field rule without writing a custom validator.
     *
     * <p>{@code @AssertTrue} (runtime, Bean Validation): the validator calls this "is..." getter-style method and
     * reports a violation (with this message) if it returns false. {@code @JsonIgnore} (runtime, Jackson): stop
     * Jackson from treating the method as a JSON property ("reasonPresentWhenRejected") when (de)serializing.
     */
    @JsonIgnore
    @AssertTrue(message = "reason is required when rejecting an application")
    public boolean isReasonPresentWhenRejected() {
        // Enums are singletons, so comparing them with == / != is correct (unlike Strings).
        // || and && short-circuit exactly as in TS, so reason.isBlank() never runs on null.
        return status != ApplicationStatus.REJECTED || (reason != null && !reason.isBlank());
    }
}
