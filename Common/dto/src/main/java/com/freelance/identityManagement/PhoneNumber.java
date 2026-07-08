package com.freelance.identityManagement;

import java.io.Serializable;

/**
 * Immutable value object representing a phone number split into its
 * country calling code and national number.
 */
public record PhoneNumber(String countryCode, String number) implements Serializable {

    /**
     * Normalizes and validates the country code and number before assignment.
     */
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

    /**
     * Formats this phone number in E.164 format, e.g. "+14155552671".
     */
    public String toE164() {
        return "+" + countryCode + number;
    }

    /**
     * Convenience factory method for creating a validated PhoneNumber.
     */
    public static PhoneNumber of(String countryCode, String nationalNumber) {
        return new PhoneNumber(countryCode, nationalNumber);
    }
}