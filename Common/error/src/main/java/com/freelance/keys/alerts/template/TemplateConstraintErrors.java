package com.freelance.keys.alerts.template;

import com.freelance.config.IError;

/**
 * Enumerates client-facing constraint violation errors for alert templates.
 */
public enum TemplateConstraintErrors implements IError {

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

    private static final String CATEGORY = "alerts.template.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    TemplateConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
        return this.indirectionCode;
    }

    @Override
    public String getDefaultMessage() {
        return this.defaultMessage;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public int getDefaultStatusCode() {
        return this.defaultStatusCode;
    }
}
