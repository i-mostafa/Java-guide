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
 *
 * <p>NestJS analogy: a provider implementing {@code OnApplicationBootstrap}; or in Express, a
 * seed function awaited before {@code app.listen()}. Spring Boot calls {@code run(...)} once,
 * after all beans are created, and only if the conditions on the class hold.
 */
@Slf4j
// @Component (runtime): register this class as a bean so Spring finds and runs it.
@Component
// @Profile("!prod") (runtime): only create this bean when the "prod" profile is NOT active.
@Profile("!prod")
// @ConditionalOnProperty (runtime): only create it when app.bootstrap-admin.enabled=true in config
// (the test profile sets it to false).
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties props;

    // Implements ApplicationRunner.run - the hook Spring Boot calls at startup.
    // @Transactional works here because Spring calls run() through the bean's proxy.
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.email().toLowerCase();
        // Idempotent: do nothing if the admin already exists (e.g. on the second start).
        if (users.existsByEmail(email)) {
            return;
        }
        users.save(User.create(email, passwordEncoder.encode(props.password()), "Platform", "Admin", Role.ADMIN));
        log.info("Bootstrap admin user created: {}", email);
    }
}
