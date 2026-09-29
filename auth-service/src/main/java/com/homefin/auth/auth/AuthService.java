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
 * Business logic lives in @Service classes. Controllers stay thin (HTTP <-> DTO mapping only).
 * Dependencies are injected through the constructor (Lombok generates it for final fields).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final ApplicationEventPublisher events;

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("email-taken", "An account with this email already exists");
        }
        User user;
        try {
            // saveAndFlush -> the INSERT runs now, so a unique-index violation surfaces here
            user = users.saveAndFlush(User.create(
                    email,
                    passwordEncoder.encode(request.password()),
                    request.firstName().trim(),
                    request.lastName().trim(),
                    Role.CUSTOMER));
        } catch (DataIntegrityViolationException raceLost) {
            // two concurrent sign-ups with the same email: the DB constraint is the real guard
            throw new ConflictException("email-taken", "An account with this email already exists");
        }

        // In-process event; the Kafka publish happens only AFTER the DB transaction commits.
        events.publishEvent(new UserRegisteredDomainEvent(user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName()));
        log.info("User registered userId={}", user.getId()); // log ids, not emails (PII)
        return user;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException ex) {
            log.info("Failed login attempt");
            throw new InvalidCredentialsException();
        }
        User user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        return TokenResponse.bearer(tokenService.issueAccessToken(user), tokenService.ttlSeconds());
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }
}
