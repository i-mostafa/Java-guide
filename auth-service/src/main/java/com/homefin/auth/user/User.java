package com.homefin.auth.user;

import com.homefin.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA entity -> table "users" (schema created by Flyway, Hibernate only VALIDATES it).
 * JPA needs a no-args constructor; we make it protected so application code must use the factory.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING) // store "CUSTOMER", never the ordinal
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    public static User create(String email, String passwordHash, String firstName, String lastName, Role role) {
        User u = new User();
        u.email = email;
        u.passwordHash = passwordHash;
        u.firstName = firstName;
        u.lastName = lastName;
        u.role = role;
        return u;
    }
}
