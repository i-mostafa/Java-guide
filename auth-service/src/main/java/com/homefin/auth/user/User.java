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
 *
 * <p>TS analogy: a TypeORM {@code @Entity('users')} class. id, created_at, updated_at and version
 * come from {@code BaseEntity}. There are no setters: state is only set through
 * {@link #create}, which keeps invariants in one place. Hibernate itself fills fields directly
 * via reflection when loading rows, so it doesn't need setters either.
 */
// @Entity (JPA, runtime): Hibernate maps this class to a table and manages its instances.
@Entity
// @Table: the table name (the default would be "user", a reserved word in PostgreSQL).
@Table(name = "users")
// @Getter (Lombok, compile time): getEmail(), getPasswordHash(), ..., isEnabled().
@Getter
// @NoArgsConstructor (Lombok, compile time): generates "protected User() {}" required by JPA.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    // @Column (JPA): column mapping. Without "name", the column name is derived from the field name.
    // unique/nullable/length describe the schema (used for validation and DDL generation).
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    // @Enumerated(STRING) (JPA): persist the enum by its name; the default (ORDINAL = 0, 1...) breaks
    // silently if someone reorders the constants.
    @Enumerated(EnumType.STRING) // store "CUSTOMER", never the ordinal
    @Column(nullable = false, length = 20)
    private Role role;

    // A field initialiser: runs whenever a User object is constructed.
    @Column(nullable = false)
    private boolean enabled = true;

    // Static factory method - the only public way to build a new User.
    public static User create(String email, String passwordHash, String firstName, String lastName, Role role) {
        User u = new User();
        // Private fields are accessible here because this code is inside the User class itself.
        u.email = email;
        u.passwordHash = passwordHash;
        u.firstName = firstName;
        u.lastName = lastName;
        u.role = role;
        return u;
    }
}
