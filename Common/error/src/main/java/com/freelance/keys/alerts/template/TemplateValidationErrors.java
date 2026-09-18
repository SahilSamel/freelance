package com.freelance.keys.alerts.template;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for alert templates and template buttons.
 */
public enum TemplateValidationErrors implements IError {

    INVALID_TEMPLATE_ID(
            "FL_7002",
            "The provided template ID is invalid or missing.",
            "Template ID validation failed",
            400
    ),
    INVALID_TEMPLATE_KEY(
            "FL_7003",
            "The provided template key is blank or improperly formatted.",
            "Template key validation failed",
            400
    ),
    INVALID_TEMPLATE_CONTENT(
            "FL_7004",
            "The template content cannot be blank.",
            "Template content validation failed",
            400
    ),
    INVALID_TEMPLATE_BUTTON(
            "FL_7008",
            "Template button configuration is invalid or missing required fields.",
            "Template button validation failed",
            400
    ),
    TEMPLATE_CHAIN_BROKEN(
            "FL_7009",
            "Template button references a reply template key that is not provided in the request or does not terminate at a terminal template or loop.",
            "Template button chain validation failed",
            400
    ),
    DUPLICATE_TEMPLATE_KEY(
            "FL_7010",
            "Duplicate template key found in the template creation request.",
            "Duplicate template key in request",
            400
    ),
    DUPLICATE_BUTTON_PAYLOAD(
            "FL_7011",
            "Duplicate button payload found in the template creation request.",
            "Duplicate button payload in request",
            400
    ),
    DUPLICATE_BUTTON_TEXT(
            "FL_7012",
            "Duplicate button text found within the same template.",
            "Duplicate button text in template",
            400
    ),
    DUPLICATE_BUTTON_SEQUENCE(
            "FL_7013",
            "Duplicate button sequence order found within the same template.",
            "Duplicate button sequence order in template",
            400
    ),
    TOO_MANY_BUTTONS(
            "FL_7014",
            "Template cannot have more than 10 buttons/options.",
            "Maximum 10 buttons/options per template exceeded",
            400
    ),
    BUTTONS_NOT_SUPPORTED_FOR_CHANNEL(
            "FL_7015",
            "Buttons are not supported for this communication channel. Buttons can only be configured for WhatsApp templates.",
            "Buttons not supported for non-WhatsApp channel",
            400
    ),
    INVALID_TEMPLATE_CATEGORY(
            "FL_7016",
            "The provided template category is invalid or not registered in the system.",
            "Template category validation failed",
            400
    ),
    INVALID_TEMPLATE_PRIORITY(
            "FL_7017",
            "The provided template priority is invalid. Allowed values are HIGH, NORMAL, or LOW.",
            "Template priority validation failed",
            400
    );

    private static final String CATEGORY = "alerts.template.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    TemplateValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
