package com.homefin.auth.config;

import com.homefin.auth.user.Role;
import com.homefin.auth.user.User;
import com.homefin.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates an ADMIN user on startup for local development.
 * ApplicationRunner = code that runs once the context is ready (like a startup hook).
 */
@Slf4j
@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.email().toLowerCase();
        if (users.existsByEmail(email)) {
            return;
        }
        users.save(User.create(email, passwordEncoder.encode(props.password()), "Platform", "Admin", Role.ADMIN));
        log.info("Bootstrap admin user created: {}", email);
    }
}
