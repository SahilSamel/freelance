package com.freelance.keys.alerts;

import com.freelance.config.IError;

public enum ConstraintErrors implements IError {

    TEMPLATE_NOT_FOUND(
            "FL_7101",
            "The requested alert template does not exist.",
            "Template lookup by ID or key failed",
            404
    ),

    TEMPLATE_NOT_APPROVED(
            "FL_7102",
            "The alert template has not been approved by WhatsApp / Meta and cannot be dispatched.",
            "Template status is not APPROVED",
            400
    ),

    TEMPLATE_KEY_ALREADY_EXISTS(
            "FL_7103",
            "An alert template with this key already exists.",
            "Template key uniqueness constraint violated",
            409
    ),

    USER_CONTACT_NOT_FOUND(
            "FL_7104",
            "No contact details found for the target user on the selected channel.",
            "Recipient contact information missing in FL_USER_CONTACT",
            404
    ),

    USER_OPTED_OUT_OF_CHANNEL(
            "FL_7105",
            "The target user has opted out or the channel is inactive.",
            "User preference blocks delivery on this channel",
            400
    ),

    WEBHOOK_SIGNATURE_INVALID(
            "FL_7106",
            "The HMAC SHA-256 signature in X-Hub-Signature-256 header is invalid.",
            "Meta webhook security handshake verification failed",
            401
    ),

    META_DISPATCH_FAILED(
            "FL_7107",
            "Failed to dispatch message to Meta WhatsApp Cloud API.",
            "Meta HTTP request failed with permanent error",
            502
    ),

    BUTTON_PAYLOAD_ALREADY_EXISTS(
            "FL_7108",
            "A button with this payload already exists in the system.",
            "Button payload uniqueness constraint violated",
            409
    ),

    TEMPLATE_CATEGORY_NOT_FOUND(
            "FL_7109",
            "The specified alert template category does not exist.",
            "Category key not found in FL_ALERT_TEMPLATE_CATEGORY",
            400
    ),

    TEMPLATE_CHAIN_BROKEN(
            "FL_7110",
            "Template button references a reply template key that does not exist or does not terminate at a terminal template or loop.",
            "Template button chain constraint violated",
            400
    ),

    META_TEMPLATE_SUBMISSION_FAILED(
            "FL_7111",
            "Failed to submit alert template to Meta WhatsApp Cloud API.",
            "Meta Graph API template submission error",
            502
    ),

    HUMAN_ESCALATION_BUTTON_REQUIRED(
            "FL_7112",
            "The default welcome menu must contain at least one option that connects to a human agent (requiresHuman=true).",
            "Mandatory human escalation button missing or invalid in welcome menu update",
            400
    );

    private static final String CATEGORY = "alerts.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    ConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
        this.indirectionCode = indirectionCode;
        this.defaultMessage = defaultMessage;
        this.description = description;
        this.defaultStatusCode = defaultStatusCode;
    }

    @Override
    public String getErrorKey() {
        return CATEGORY + "." + name();
    }

    @Override
    public String getIndirectionCode() {
        return indirectionCode;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getDefaultStatusCode() {
        return defaultStatusCode;
    }
}