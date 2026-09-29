package com.homefin.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on JPA auditing: automatic filling of the {@code createdAt} / {@code updatedAt} columns declared in
 * common-lib's BaseEntity (fields annotated {@code @CreatedDate} / {@code @LastModifiedDate}).
 *
 * <p>The class body is empty on purpose: the annotations are the whole point. Spring reads them at
 * startup. TS analogy: TypeORM's {@code @CreateDateColumn} / {@code @UpdateDateColumn}, which here must be
 * switched on once for the whole app.
 */
// @Configuration (runtime): a Spring configuration class, picked up by component scanning.
@Configuration
// @EnableJpaAuditing (runtime): registers the auditing handler that sets the timestamps before
// each INSERT/UPDATE.
@EnableJpaAuditing
public class JpaConfig {
}
