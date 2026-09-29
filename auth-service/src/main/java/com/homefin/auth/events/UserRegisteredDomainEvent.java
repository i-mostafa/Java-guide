package com.homefin.auth.events;

import java.util.UUID;

/** Internal (in-JVM) Spring application event. Not the Kafka contract. */
public record UserRegisteredDomainEvent(UUID userId, String email, String firstName, String lastName) {
}
