package com.freelance.alerts.template;

import com.freelance.alerts.AlertChannel;

import java.util.List;
import java.util.Set;

/**
 * Common contract for code-defined alert and notification templates across all domain modules.
 * <p>
 * <b>Mandated Package Convention:</b><br/>
 * All implementations (classes or enums) of {@link ITemplate} MUST reside under package
 * {@code com.freelance.alerts.templates} (or its subpackages) to be discovered and registered
 * automatically via classpath reflection scanning during application startup.
 * </p>
 */
public interface ITemplate {

    /**
     * Unique identifier key for this template across the system.
     * <p>
     * <b>Format:</b> Uppercase alphanumeric with underscores (e.g., {@code "IDENTITY_WELCOME"},
     * {@code "AUTH_PASSWORD_RESET"}, {@code "BILLING_INVOICE_PAID"}).
     * </p>
     *
     * @return non-blank unique template key string
     */
    String getTemplateKey();

    /**
     * Human-readable name or title of the template used for administrative identification.
     * <p>
     * <b>Example:</b> {@code "User Onboarding Welcome Notification"}
     * </p>
     *
     * @return non-blank human-readable name
     */
    String getName();

    /**
     * Meta WhatsApp template classification category.
     * <p>
     * <b>Common Values:</b>
     * <ul>
     *   <li>{@code "AUTHENTICATION"} - OTPs, 2FA, password reset codes.</li>
     *   <li>{@code "UTILITY"} - Transaction confirmations, billing receipts, account updates.</li>
     *   <li>{@code "MARKETING"} - Promotions, offers, newsletters.</li>
     * </ul>
     * </p>
     *
     * @return template category string (defaults to "UTILITY" if null)
     */
    String getCategory();

    /**
     * ISO 639-1 language / locale code for message localization.
     * <p>
     * <b>Format:</b> 2-letter language code or language_locale (e.g., {@code "en"}, {@code "en_US"}, {@code "es"}).
     * </p>
     *
     * @return language code string (defaults to "en" if null)
     */
    String getLanguage();

    /**
     * Main message body content template.
     * <p>
     * Dynamic attributes should be enclosed in double curly braces (e.g., {@code "Hello {{USER.username}}, your OTP is {{AUTH.otp}}."}).
     * Placeholders must match attributes returned by {@link #getRequiredAttributes()} and registered {@code IDomainAttributeResolver} implementations.
     * </p>
     *
     * @return message body text with optional {{domain.attribute}} placeholders
     */
    String getContent();

    /**
     * Optional public HTTP/HTTPS URL for media header attachments (image, document, video).
     *
     * @return media URL string, or {@code null} if no header media is attached
     */
    String getMediaUrl();

    /**
     * Media format type corresponding to the attached media file.
     * <p>
     * <b>Supported Values:</b> {@code "IMAGE"}, {@code "DOCUMENT"}, {@code "VIDEO"}.
     * </p>
     *
     * @return media type string, or {@code null} if no header media is attached
     */
    String getMediaType();

    /**
     * Target communication channel for this template.
     *
     * @return target channel (e.g., {@link AlertChannel#WHATSAPP})
     */
    AlertChannel getChannel();

    /**
     * Set of dynamic placeholder attribute keys required by this template.
     * <p>
     * <b>Example:</b> {@code Set.of("USER.username", "USER.phone", "PAYMENT.amount")}
     * </p>
     *
     * @return set of required attribute keys, or empty set / {@code null} if purely static
     */
    Set<String> getRequiredAttributes();

    /**
     * Ordered collection of interactive buttons attached to the bottom of the message.
     * <p>
     * Supports Quick Reply and CTA buttons (URL / Phone). WhatsApp limits messages to at most
     * 3 Quick Reply buttons or 2 CTA buttons.
     * </p>
     *
     * @return list of button definitions, or empty list / {@code null} if no buttons
     */
    List<ITemplateButton> getButtons();

    /**
     * Optional pre-approved Meta WhatsApp Template ID.
     * <p>
     * Seeded system templates are developer-managed and assumed pre-approved in Meta Business Manager.
     * Developers can optionally declare the pre-approved Meta Template ID here.
     * </p>
     *
     * @return Meta template ID string, or {@code null} if not assigned or not applicable
     */
    default String getMetaTemplateId() {
        return null;
    }

    /**
     * Optional explicit priority for internal asynchronous event and dispatcher queuing.
     * <p>
     * Allowed values: {@code "HIGH"}, {@code "NORMAL"}, {@code "LOW"}.
     * If {@code null}, priority falls back to the assigned category's default priority.
     * </p>
     *
     * @return priority string, or {@code null} to inherit category default priority
     */
    default String getPriority() {
        return null;
    }
}