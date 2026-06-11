package IdentityManagement;

import java.io.Serializable;

public record PhoneNumber(String countryCode, String nationalNumber) implements Serializable {

    public PhoneNumber {
        if (countryCode == null || nationalNumber == null) {
            throw new IllegalArgumentException("Country code and national number cannot be null");
        }

        countryCode = countryCode.trim().replace("+", "");
        nationalNumber = nationalNumber.replaceAll("[^0-9]", "");

        if (countryCode.isEmpty() || nationalNumber.isEmpty()) {
            throw new IllegalArgumentException("Invalid phone number components provided");
        }
    }

    public String toE164() {
        return "+" + countryCode + nationalNumber;
    }

    public static PhoneNumber of(String countryCode, String nationalNumber) {
        return new PhoneNumber(countryCode, nationalNumber);
    }
}