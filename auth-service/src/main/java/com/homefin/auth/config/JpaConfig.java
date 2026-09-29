package com.homefin.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Switches on JPA auditing, which fills the {@code @CreatedDate}/{@code @LastModifiedDate} fields
 * of {@code BaseEntity} (common-lib) automatically on insert/update.
 *
 * <p>The class body is empty on purpose: the annotations are the whole point. Kept in its own
 * config class (not on the main application class) so sliced tests can leave it out.
 */
// @Configuration: a configuration class picked up by component scanning.
@Configuration
// @EnableJpaAuditing (Spring Data, runtime): registers the auditing handler used by AuditingEntityListener.
@EnableJpaAuditing
public class JpaConfig {
}
