package com.example.core.validator;

import java.util.Collection;

/**
 * Validates participant names and collection limits for the roulette wheel.
 */
public final class NameValidator {

    public static final int DEFAULT_MAX_NAME_LENGTH = 50;
    public static final int DEFAULT_MAX_PARTICIPANTS = 40;
    public static final int MIN_PARTICIPANTS_FOR_SPIN = 2;

    private NameValidator() {}

    /**
     * Trims leading and trailing whitespace from the string. Returns an empty
     * string if input is null.
     */
    public static String cleanName(String raw) {
        return (raw == null) ? "" : raw.trim();
    }

    /**
     * Checks if the given name is null or blank.
     */
    public static boolean isBlank(String name) {
        return name == null || name.trim().isEmpty();
    }

    /**
     * Checks if the name is valid (non-blank and within maximum length).
     */
    public static boolean isValidName(String name) {
        return isValidName(name, DEFAULT_MAX_NAME_LENGTH);
    }

    /**
     * Checks if the name is valid (non-blank and within specified maximum length).
     */
    public static boolean isValidName(String name, int maxLength) {
        String cleaned = cleanName(name);
        return !cleaned.isEmpty() && cleaned.length() <= maxLength;
    }

    /**
     * Validates whether a name can be added given the current participant count.
     *
     * @param raw             raw name entered by user
     * @param currentCount    current number of participants on the wheel
     * @param maxParticipants maximum allowed participants on the wheel
     * @return {@link ValidationResult} with success flag and descriptive error message
     */
    public static ValidationResult validateForAdd(String raw, int currentCount, int maxParticipants) {
        String cleaned = cleanName(raw);
        if (cleaned.isEmpty()) {
            return ValidationResult.failure("Type a name first.");
        }
        if (cleaned.length() > DEFAULT_MAX_NAME_LENGTH) {
            return ValidationResult.failure("Name too long — max " + DEFAULT_MAX_NAME_LENGTH + " characters.");
        }
        if (currentCount >= maxParticipants) {
            return ValidationResult.failure("Limit reached — max " + maxParticipants + " participants.");
        }
        return ValidationResult.success(cleaned);
    }

    /**
     * Overload using {@link #DEFAULT_MAX_PARTICIPANTS}.
     */
    public static ValidationResult validateForAdd(String raw, int currentCount) {
        return validateForAdd(raw, currentCount, DEFAULT_MAX_PARTICIPANTS);
    }

    /**
     * Checks whether the wheel has enough participants to spin.
     */
    public static boolean canSpin(int participantCount) {
        return participantCount >= MIN_PARTICIPANTS_FOR_SPIN;
    }

    /**
     * Checks whether the collection has enough participants to spin.
     */
    public static boolean canSpin(Collection<?> participants) {
        return participants != null && participants.size() >= MIN_PARTICIPANTS_FOR_SPIN;
    }

    /**
     * Result of a name validation check.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String cleanedName;
        private final String errorMessage;

        private ValidationResult(boolean valid, String cleanedName, String errorMessage) {
            this.valid = valid;
            this.cleanedName = cleanedName;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success(String cleanedName) {
            return new ValidationResult(true, cleanedName, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, null, errorMessage);
        }

        public boolean isValid()           { return valid; }
        public String getCleanedName()     { return cleanedName; }
        public String getErrorMessage()    { return errorMessage; }
    }
}
