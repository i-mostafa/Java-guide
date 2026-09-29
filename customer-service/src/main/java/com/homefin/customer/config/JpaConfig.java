package com.homefin.customer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Switches on JPA auditing: automatic filling of timestamp fields on entities.
 *
 * <p>{@code @Configuration} (runtime): marks a class Spring reads at startup for bean definitions and settings,
 * like a NestJS module's providers array. Here the class body is empty; the annotations ARE the configuration.
 *
 * <p>{@code @EnableJpaAuditing} (runtime): registers a listener so that entity fields annotated with
 * {@code @CreatedDate} / {@code @LastModifiedDate} are set automatically on insert/update. Similar to TypeORM's
 * {@code @CreateDateColumn} / {@code @UpdateDateColumn}. It lives in its own class (not on the main application
 * class) so that sliced tests like {@code @WebMvcTest}, which don't start JPA, don't trip over it.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
