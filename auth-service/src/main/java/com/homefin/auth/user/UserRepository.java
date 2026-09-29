package com.homefin.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data generates the implementation at runtime from method names
 * (findByEmail -> "select u from User u where u.email = ?1"). No boilerplate DAO code.
 *
 * <p>TS analogy: TypeORM's {@code Repository<User>} / Prisma's {@code prisma.user} - but you only
 * write the interface. At startup Spring Data creates a proxy object implementing it and registers
 * it as a bean, so it can be injected into services.
 */
// An interface declares method signatures without bodies (like a TS interface).
// JpaRepository<User, UUID>: generic parameters = entity type and id type. It brings save(),
// saveAndFlush(), findById(), findAll(), delete(), paging and more for free.
public interface UserRepository extends JpaRepository<User, UUID> {

    // Optional<User> = "maybe a User": forces callers to handle the not-found case explicitly.
    Optional<User> findByEmail(String email);

    // Derived query: "select count(*) > 0 ... where email = ?".
    boolean existsByEmail(String email);
}
