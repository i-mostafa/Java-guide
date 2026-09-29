package com.homefin.customer.customer;

import com.homefin.customer.customer.validation.Adult;
import com.homefin.customer.customer.validation.AdultValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AdultValidatorTest {

    private final AdultValidator validator = new AdultValidator();

    @ParameterizedTest(name = "{0} years old -> valid={1}")
    @CsvSource({"17, false", "18, true", "40, true"})
    void checksMinimumAge(int years, boolean expected) {
        validator.initialize(adult18());
        LocalDate dob = LocalDate.now().minusYears(years);
        assertThat(validator.isValid(dob, null)).isEqualTo(expected);
    }

    @Test
    void nullIsLeftToNotNull() {
        validator.initialize(adult18());
        assertThat(validator.isValid(null, null)).isTrue();
    }

    /** Grab a real annotation instance (default minAge = 18) from a field. */
    private static class Holder {
        @Adult
        LocalDate dob;
    }

    private static Adult adult18() {
        try {
            return Holder.class.getDeclaredField("dob").getAnnotation(Adult.class);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
