package com.freelance.identityManagement;

import java.io.Serializable;

/**
 * Standardizes phone numbers across the application to adhere to a consistent structure.
 *
 * @param countryCode country calling code without '+' (e.g., "1", "91")
 * @param number      national subscriber number containing only digits
 */
public record PhoneNumber(String countryCode, String number) implements Serializable {

    public PhoneNumber {
        if (countryCode == null || number == null) {
            throw new IllegalArgumentException("Country code and national number cannot be null");
        }

        countryCode = countryCode.trim().replace("+", "");
        number = number.replaceAll("[^0-9]", "");

        if (countryCode.isEmpty() || number.isEmpty()) {
            throw new IllegalArgumentException("Invalid phone number components provided");
        }
    }

    public String toE164() {
        return "+" + countryCode + number;
    }

    /**
     * Creates a normalized {@link PhoneNumber} instance.
     *
     * @param countryCode    valid country calling code (e.g., "1", "91"); must not be null or blank
     * @param nationalNumber valid national subscriber number containing numeric digits; must not be null or blank
     * @return normalized {@link PhoneNumber} instance
     * @throws IllegalArgumentException if either country code or national number is null, empty, or contains no digits
     */
    public static PhoneNumber of(String countryCode, String nationalNumber) {
        return new PhoneNumber(countryCode, nationalNumber);
    }
}