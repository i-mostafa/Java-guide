package com.homefin.customer.customer;

import com.homefin.customer.customer.validation.Adult;
import com.homefin.customer.customer.validation.AdultValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

// "import static" imports a static METHOD so it can be called without the class name: assertThat(x) instead of
// Assertions.assertThat(x). Like import { expect } from '@jest/globals'.
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test for {@link AdultValidator}, using JUnit 5 (the test runner, like jest) and AssertJ (fluent
 * assertions: {@code assertThat(actual).isEqualTo(expected)} is {@code expect(actual).toBe(expected)}).
 * No Spring context: the validator is created with {@code new} and called directly.
 *
 * <p>JUnit discovers the class by convention (name ends in "Test", under src/test/java) and runs every method
 * annotated with {@code @Test} or {@code @ParameterizedTest}. The class and methods are package-private (no
 * {@code public}), which is enough for JUnit 5 and is the usual style.
 */
class AdultValidatorTest {

    // A new instance of the test class is created for EACH test method, so this field is fresh per test
    // (like creating it in beforeEach).
    private final AdultValidator validator = new AdultValidator();

    // @ParameterizedTest (runtime, JUnit): run this method once per row of arguments, like jest's test.each.
    // name = display name template; {0}/{1} are the arguments.
    // @CsvSource: each string is one row of comma-separated values, converted to the parameter types (int, boolean).
    @ParameterizedTest(name = "{0} years old -> valid={1}")
    @CsvSource({"17, false", "18, true", "40, true"})
    void checksMinimumAge(int years, boolean expected) {
        validator.initialize(adult18());
        LocalDate dob = LocalDate.now().minusYears(years);
        // The context argument isn't used by our validator, so null is fine in a unit test.
        assertThat(validator.isValid(dob, null)).isEqualTo(expected);
    }

    // @Test (runtime, JUnit): a single test case, like it('...', () => {...}). The method name is the description.
    @Test
    void nullIsLeftToNotNull() {
        validator.initialize(adult18());
        assertThat(validator.isValid(null, null)).isTrue();
    }

    /** Grab a real annotation instance (default minAge = 18) from a field. */
    // A nested class used only as a carrier for an annotated field. "static" nested class = doesn't need an instance
    // of the outer class. Annotations can't be created with "new", so we read one from this field via reflection.
    private static class Holder {
        @Adult
        LocalDate dob;
    }

    private static Adult adult18() {
        // getDeclaredField throws the CHECKED exception NoSuchFieldException, so Java forces us to catch it (or
        // declare "throws"). We wrap it in an unchecked exception because it can't happen here.
        try {
            return Holder.class.getDeclaredField("dob").getAnnotation(Adult.class);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
