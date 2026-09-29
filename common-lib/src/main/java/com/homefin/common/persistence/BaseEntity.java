package com.homefin.common.persistence;

// jakarta.persistence = JPA, the standard Java ORM API (annotations + EntityManager).
// Hibernate is the implementation Spring Boot uses. Think "TypeORM decorators", standardized.
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Common columns for all entities:
 *  - UUID primary key (safe to expose, no enumeration attacks, generated in the app)
 *  - audit timestamps filled by Spring Data ({@code @EnableJpaAuditing} must be on in the service)
 *  - {@code @Version} for optimistic locking -> concurrent updates fail instead of silently overwriting
 *
 * equals/hashCode follow the Hibernate-safe "id-based" pattern (see Vlad Mihalcea's article
 * linked in the guide). Never use Lombok {@code @Data} / {@code @EqualsAndHashCode} on entities.
 *
 * <p>TS analogy: an abstract TypeORM base entity with {@code @PrimaryGeneratedColumn('uuid')},
 * {@code @CreateDateColumn}, {@code @UpdateDateColumn} and {@code @VersionColumn} that every
 * entity class extends. All annotations here are read by Hibernate at runtime (startup) to build
 * the table mapping; only {@code @Getter} is compile-time (Lombok).
 */
// @Getter (Lombok, compile time): generates getId(), getCreatedAt(), getUpdatedAt(), getVersion().
// No setters are generated on purpose: these fields are managed by Hibernate/Spring, not by our code.
@Getter
// @MappedSuperclass (JPA, runtime): "this is not a table itself; its mapped fields are copied
// into the table of every @Entity that extends it".
@MappedSuperclass
// @EntityListeners (JPA, runtime): registers lifecycle callbacks. AuditingEntityListener fills the
// @CreatedDate/@LastModifiedDate fields just before INSERT/UPDATE (like TypeORM subscribers).
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    // @Id: primary key. @GeneratedValue(UUID): Hibernate generates a random UUID on first save.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // @CreatedDate (Spring Data): set once on insert. @Column maps the field to a DB column;
    // updatable = false means Hibernate never includes it in UPDATE statements.
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // @LastModifiedDate (Spring Data): refreshed on every update.
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // @Version (JPA): Hibernate adds "WHERE version = ?" to every UPDATE and increments it.
    // If 0 rows match, someone else changed the row -> OptimisticLockingFailureException.
    // "long" is a primitive 64-bit integer.
    @Version
    private long version;

    // Every Java object has equals()/hashCode() inherited from Object; collections (Set, Map keys)
    // use them for equality. We override them. "final" on a method = subclasses cannot override it.
    @Override
    public final boolean equals(Object o) {
        // == on objects compares references (identity), like === on JS objects.
        if (this == o) {
            return true;
        }
        // Hibernate may wrap entities in runtime-generated "proxy" subclasses (for lazy loading),
        // so compare the real underlying classes instead of o.getClass().
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        // A cast: tell the compiler "treat o as a BaseEntity" (checked at runtime), like "o as BaseEntity".
        BaseEntity other = (BaseEntity) o;
        // Unsaved entities (id == null) are never equal to anything but themselves.
        return id != null && id.equals(other.getId());
    }

    // Constant per class so the hash doesn't change when the id is assigned on save.
    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
