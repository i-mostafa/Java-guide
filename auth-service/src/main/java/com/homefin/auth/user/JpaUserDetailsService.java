package com.homefin.auth.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tells Spring Security how to load a user by "username" (our email) during login.
 *
 * <p>Implementing the {@code UserDetailsService} interface is how you plug your own user store into
 * Spring Security; Boot detects this bean and wires it into the AuthenticationManager. Node analogy:
 * the lookup inside a passport-local strategy: {@code new LocalStrategy(async (email, pw, done) => ...)}.
 * Spring then compares the password hash itself.
 */
@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    // Called by Spring Security during authenticationManager.authenticate(...).
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = users.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        // Fully-qualified class name: our own entity is also called "User", so Spring Security's User
        // class is referenced by its full package path instead of an import (Java has no "import as").
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                // isEnabled(): Lombok names getters of primitive boolean fields "isX" instead of "getX".
                .disabled(!user.isEnabled())
                .build();
    }
}
