package com.freelance.keys.gym;

import com.freelance.config.IConfig;

/**
 * System and tenant configuration keys defining gym operational profile, contact, and self-service details.
 */
public enum GymProfileConfigKeys implements IConfig {

    GYM_TIMINGS(
            "gym.profile.timings",
            "Mon-Sat 6:00 AM - 10:00 PM",
            "Gym general operational hours and schedule"
    ),

    GYM_SUPPORT_PHONE(
            "gym.profile.support_phone",
            "+91 98765 43210",
            "Front desk / reception WhatsApp contact number"
    ),

    GYM_ADDRESS(
            "gym.profile.address",
            "Platinum Hub, Link Road",
            "Physical street address of the gym facility"
    ),

    GYM_GOOGLE_MAPS_URL(
            "gym.profile.google_maps_url",
            "https://maps.google.com",
            "Google Maps location or review URL for the gym"
    ),

    GYM_WHATSAPP_CHANNEL_URL(
            "gym.profile.whatsapp_channel_url",
            "https://whatsapp.com/channel/ironpulse",
            "Official WhatsApp Community Channel link for announcements and daily workouts"
    ),

    GYM_UPI_QR_MEDIA_ID(
            "gym.profile.upi_qr_media_id",
            "",
            "Media ID or URL of the gym UPI payment QR code image"
    ),

    GYM_BILLING_ADDRESS(
            "gym.profile.billing_address",
            "Platinum Hub, Link Road, Mumbai, Maharashtra 400053",
            "Official billing address of the gym printed on invoices and receipts"
    ),

    GYM_GSTIN(
            "gym.profile.gstin",
            "27AAAAA0000A1Z5",
            "Gym GSTIN tax identification number displayed on tax invoices and receipts"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    GymProfileConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    GymProfileConfigKeys(String key, String defaultValue, String description, String validation) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
        this.validation = validation;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getValidation() {
        return validation;
    }
}
