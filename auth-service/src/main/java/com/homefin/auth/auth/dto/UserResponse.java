package com.homefin.auth.auth.dto;

import com.homefin.auth.user.User;

import java.util.UUID;

/**
 * Public JSON view of a user (never exposes the password hash).
 *
 * <p>Mapping entity -> DTO keeps the API shape independent of the DB model, like returning a
 * plain object from a TypeORM entity instead of the entity itself.
 */
public record UserResponse(UUID id, String email, String firstName, String lastName, String role) {

    // Static mapper; u.getId() etc. are Lombok-generated getters on the User entity.
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole().name());
    }
}
