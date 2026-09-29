package com.homefin.customer.customer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom field-level constraint: the date must be at least {@code minAge} years ago.
 *
 * <p>{@code public @interface Adult} declares a new ANNOTATION type (not an interface you implement). Usage elsewhere:
 * {@code @Adult LocalDate dateOfBirth} or {@code @Adult(minAge = 21)}. Similar to writing a custom class-validator
 * decorator in TS, except a Java annotation is pure metadata: it has no behavior of its own. The behavior lives in
 * {@link AdultValidator}, linked via the {@code @Constraint} meta-annotation below.
 *
 * <p>Meta-annotations (annotations on an annotation):
 * <ul>
 *   <li>{@code @Documented}: show this annotation in generated Javadoc of annotated elements.</li>
 *   <li>{@code @Constraint(validatedBy = ...)}: tells Bean Validation which class checks it (at runtime).</li>
 *   <li>{@code @Target}: where it may be placed: fields, method parameters, and record components.
 *       Putting it elsewhere is a compile error.</li>
 *   <li>{@code @Retention(RUNTIME)}: keep it in the compiled class file so it can be read by reflection at runtime.
 *       Without it the annotation would be discarded after compiling and the validator would never run.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = AdultValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Adult {

    // Annotation attributes look like methods; "default" gives the value used when it isn't specified.
    int minAge() default 18;

    // The following three attributes are REQUIRED by the Bean Validation spec for every constraint annotation.
    // Error message template; {minAge} is interpolated from the attribute above.
    String message() default "customer must be at least {minAge} years old";

    // Validation groups (run only a subset of constraints). Class<?>[] = array of classes of any type;
    // the "?" wildcard means "some unknown type", like Class<unknown> / Class<any>.
    Class<?>[] groups() default {};

    // Custom metadata for clients. "? extends Payload" = any class that is Payload or a subtype of it.
    Class<? extends Payload>[] payload() default {};
}
