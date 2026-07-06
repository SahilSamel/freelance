package com.freelance.identityManagement;

import java.io.Serializable;

public record PhoneNumber(String countryCode, String nationalNumber) implements Serializable {

    public PhoneNumber {
        // Fail fast so every PhoneNumber instance is safe to serialize and format.
        if (countryCode == null || nationalNumber == null) {
            throw new IllegalArgumentException("Country code and national number cannot be null");
        }

        // Store only the dialing digits; presentation characters belong at the UI boundary.
        countryCode = countryCode.trim().replace("+", "");
        nationalNumber = nationalNumber.replaceAll("[^0-9]", "");

        if (countryCode.isEmpty() || nationalNumber.isEmpty()) {
            throw new IllegalArgumentException("Invalid phone number components provided");
        }
    }

    public String toE164() {
        // E.164 keeps the shared DTO compatible with APIs that expect international phone numbers.
        return "+" + countryCode + nationalNumber;
    }

    public static PhoneNumber of(String countryCode, String nationalNumber) {
        return new PhoneNumber(countryCode, nationalNumber);
    }
}
