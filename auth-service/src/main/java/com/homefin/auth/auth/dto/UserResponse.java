package com.homefin.auth.auth.dto;

import com.homefin.auth.user.User;

import java.util.UUID;

public record UserResponse(UUID id, String email, String firstName, String lastName, String role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole().name());
    }
}
