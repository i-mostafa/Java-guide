package com.homefin.common.events;

/**
 * Kafka topic names are part of the public contract between services.
 * Convention: {domain}.{entity}.{event}.v{version}. Bump the version on breaking changes.
 */
public final class Topics {

    public static final String USER_REGISTERED = "homefin.auth.user-registered.v1";
    public static final String APPLICATION_SUBMITTED = "homefin.application.submitted.v1";
    public static final String APPLICATION_STATUS_CHANGED = "homefin.application.status-changed.v1";

    private Topics() {
    }
}
