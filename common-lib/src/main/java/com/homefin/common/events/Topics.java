package com.homefin.common.events;

/**
 * Kafka topic names are part of the public contract between services.
 * Convention: {domain}.{entity}.{event}.v{version}. Bump the version on breaking changes.
 *
 * <p>TS analogy: {@code export const Topics = { USER_REGISTERED: '...', ... } as const}.
 * Java has no top-level constants, so they live as static fields on a class.
 */
// final class = cannot be subclassed (like a sealed/frozen class).
public final class Topics {

    // public static final = a globally accessible constant, used as Topics.USER_REGISTERED.
    public static final String USER_REGISTERED = "homefin.auth.user-registered.v1";
    public static final String APPLICATION_SUBMITTED = "homefin.application.submitted.v1";
    public static final String APPLICATION_STATUS_CHANGED = "homefin.application.status-changed.v1";

    // A private constructor prevents "new Topics()": this class is just a namespace for constants.
    private Topics() {
    }
}
