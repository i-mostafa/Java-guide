package com.homefin.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data generates the implementation at runtime from method names
 * (findByEmail -> "select u from User u where u.email = ?1"). No boilerplate DAO code.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
