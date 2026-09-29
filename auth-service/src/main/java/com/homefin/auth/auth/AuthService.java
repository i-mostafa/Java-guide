package com.homefin.auth.auth;

import com.homefin.auth.auth.dto.LoginRequest;
import com.homefin.auth.auth.dto.RegisterRequest;
import com.homefin.auth.auth.dto.TokenResponse;
import com.homefin.auth.events.UserRegisteredDomainEvent;
import com.homefin.auth.user.Role;
import com.homefin.auth.user.User;
import com.homefin.auth.user.UserRepository;
import com.homefin.common.error.ConflictException;
import com.homefin.common.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic lives in {@code @Service} classes. Controllers stay thin (HTTP <-> DTO mapping only).
 * Dependencies are injected through the constructor (Lombok generates it for final fields).
 *
 * <p>NestJS analogy: an {@code @Injectable()} AuthService. Spring creates one instance (a singleton)
 * at startup, passing in the five beans below. Because some methods are {@code @Transactional},
 * Spring actually injects a runtime-generated "proxy" subclass into the controller: the proxy opens
 * a DB transaction, calls the real method, then commits (or rolls back on an exception).
 */
// @Slf4j (Lombok, compile time): generates the static "log" logger field.
@Slf4j
// @Service (Spring, runtime): a @Component meant for business logic; registered as a singleton bean.
@Service
// @RequiredArgsConstructor (Lombok, compile time): constructor for all final fields -> Spring injects them.
@RequiredArgsConstructor
public class AuthService {

    // All dependencies are interfaces or beans; Spring decides the concrete implementation.
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    // Spring's in-process event bus (like a Node EventEmitter shared across the app).
    private final ApplicationEventPublisher events;

    // @Transactional (Spring, runtime via the proxy): the whole method runs in one DB transaction;
    // an unchecked exception thrown out of it triggers a rollback.
    @Transactional
    public User register(RegisterRequest request) {
        // request.email() is the record accessor (records don't use a "get" prefix).
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            // "throw new X(...)" works like JS; ConflictException is unchecked so no "throws" clause needed.
            throw new ConflictException("email-taken", "An account with this email already exists");
        }
        // Declared here, assigned in the try block, so it is still in scope after the try/catch.
        User user;
        try {
            // saveAndFlush -> the INSERT runs now, so a unique-index violation surfaces here
            user = users.saveAndFlush(User.create(
                    email,
                    passwordEncoder.encode(request.password()),
                    request.firstName().trim(),
                    request.lastName().trim(),
                    Role.CUSTOMER));
        // catch (Type name): only catches that exception type (and subclasses), unlike JS's catch-all.
        } catch (DataIntegrityViolationException raceLost) {
            // two concurrent sign-ups with the same email: the DB constraint is the real guard
            throw new ConflictException("email-taken", "An account with this email already exists");
        }

        // In-process event; the Kafka publish happens only AFTER the DB transaction commits.
        // (See UserEventsPublisher and its @TransactionalEventListener.)
        events.publishEvent(new UserRegisteredDomainEvent(user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName()));
        log.info("User registered userId={}", user.getId()); // log ids, not emails (PII)
        return user;
    }

    // readOnly = true: a hint that lets Hibernate skip dirty-checking and the DB optimise the transaction.
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            // Spring Security checks the password: it loads the user via JpaUserDetailsService and
            // compares hashes with the PasswordEncoder. Throws AuthenticationException on failure.
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException ex) {
            log.info("Failed login attempt");
            throw new InvalidCredentialsException();
        }
        // findByEmail returns Optional<User> - a container that is either empty or holds a value,
        // Java's explicit alternative to "User | undefined". orElseThrow(...) unwraps it or throws.
        // "InvalidCredentialsException::new" is a constructor reference, i.e. () -> new InvalidCredentialsException().
        User user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        return TokenResponse.bearer(tokenService.issueAccessToken(user), tokenService.ttlSeconds());
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        // Lambda with no parameters: () -> ..., same as a JS arrow function.
        return users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }
}
