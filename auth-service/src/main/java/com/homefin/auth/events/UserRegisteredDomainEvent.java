package com.homefin.auth.events;

import java.util.UUID;

/**
 * Internal (in-JVM) Spring application event. Not the Kafka contract.
 *
 * <p>Published via ApplicationEventPublisher in AuthService and received by
 * UserEventsPublisher - like {@code emitter.emit('user-registered', payload)} in Node. Any object
 * can be an event; a record is a convenient immutable payload. Keeping it separate from the
 * Kafka event (common-lib) lets the internal shape change without breaking other services.
 */
public record UserRegisteredDomainEvent(UUID userId, String email, String firstName, String lastName) {
}
