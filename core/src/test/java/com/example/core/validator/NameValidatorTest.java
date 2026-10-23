package com.example.core.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NameValidatorTest {

    @Test
    @DisplayName("cleanName trims whitespace and handles null")
    void cleanName() {
        assertEquals("Alice", NameValidator.cleanName("  Alice  "));
        assertEquals("Bob Smith", NameValidator.cleanName("Bob Smith"));
        assertEquals("", NameValidator.cleanName("   "));
        assertEquals("", NameValidator.cleanName(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("isBlank returns true for null or empty/whitespace-only strings")
    void isBlank(String input) {
        assertTrue(NameValidator.isBlank(input));
    }

    @Test
    @DisplayName("validateForAdd succeeds for valid names")
    void validateForAddSuccess() {
        NameValidator.ValidationResult result = NameValidator.validateForAdd("  Charlie  ", 5);
        assertTrue(result.isValid());
        assertEquals("Charlie", result.getCleanedName());
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("validateForAdd fails on empty name")
    void validateForAddEmpty() {
        NameValidator.ValidationResult result = NameValidator.validateForAdd("   ", 5);
        assertFalse(result.isValid());
        assertEquals("Type a name first.", result.getErrorMessage());
    }

    @Test
    @DisplayName("validateForAdd fails on long name > 50 chars")
    void validateForAddTooLong() {
        String longName = "A".repeat(51);
        NameValidator.ValidationResult result = NameValidator.validateForAdd(longName, 5);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("max 50 characters"));
    }

    @Test
    @DisplayName("validateForAdd fails when participant limit reached")
    void validateForAddLimitReached() {
        NameValidator.ValidationResult result = NameValidator.validateForAdd("Alice", 40, 40);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("max 40 participants"));
    }

    @Test
    @DisplayName("canSpin requires at least 2 participants")
    void canSpinChecks() {
        assertFalse(NameValidator.canSpin(0));
        assertFalse(NameValidator.canSpin(1));
        assertTrue(NameValidator.canSpin(2));
        assertTrue(NameValidator.canSpin(10));

        assertFalse(NameValidator.canSpin(List.of()));
        assertFalse(NameValidator.canSpin(List.of("Alice")));
        assertTrue(NameValidator.canSpin(List.of("Alice", "Bob")));
    }
}
