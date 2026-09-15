package com.freelance.alerts.template;

import com.freelance.alerts.ButtonType;

/**
 * Contract representing an interactive button attached to an {@link ITemplate}.
 * <p>
 * Supports WhatsApp interactive components including Quick Reply buttons and
 * Call-to-Action (CTA) buttons (URL / Phone Number).
 * </p>
 */
public interface ITemplateButton {

    /**
     * User-visible label text displayed on the button.
     * <p>
     * <b>Constraints:</b> Maximum 20–25 characters (enforced by WhatsApp / Meta API).
     * </p>
     *
     * @return non-blank button display text (e.g., {@code "Confirm"}, {@code "Contact Support"})
     */
    String getText();

    /**
     * Functional type of the interactive button.
     *
     * @return {@link ButtonType#QUICK_REPLY}, {@link ButtonType#URL}, or {@link ButtonType#PHONE_NUMBER}
     */
    ButtonType getType();

    /**
     * Technical payload or destination target for the button.
     * <p>
     * <b>Format depends on {@link #getType()}:</b>
     * <ul>
     *   <li>{@code QUICK_REPLY}: Custom opaque developer payload string returned via webhook (e.g., {@code "BTN_ACCEPT_TERMS"}).</li>
     *   <li>{@code URL}: Target web URL string (e.g., {@code "https://freelance.com/verify?token={{token}}"}).</li>
     *   <li>{@code PHONE_NUMBER}: International phone number string with country code (e.g., {@code "+1234567890"}).</li>
     * </ul>
     * </p>
     *
     * @return developer payload or destination target string
     */
    String getPayload();

    /**
     * Unique template key of a follow-up {@link ITemplate} to automatically trigger and dispatch
     * when the recipient clicks this button.
     * <p>
     * Only applicable for {@link ButtonType#QUICK_REPLY} buttons.
     * </p>
     *
     * @return target templateKey string to dispatch upon click, or {@code null} if no automated reply is chained
     */
    String getReplyTemplateKey();

    /**
     * Indicates whether clicking this button routes the conversation to a human support staff member.
     * <p>
     * When {@code true}, clicking this button activates the 24-hour sliding human support session
     * ({@code human_session_expires_at = now + 24h}), holds subsequent non-HIGH priority automated
     * dispatches for 24 hours, and surfaces the chat in the staff timeline inbox.
     * </p>
     *
     * @return {@code true} if human staff intervention is required upon click, {@code false} otherwise
     */
    boolean requiresHumanAgent();

    /**
     * 0-indexed sequence ordering index determining the visual placement of this button in the button list.
     *
     * @return 0-based ordering integer (e.g., {@code 0}, {@code 1}, {@code 2})
     */
    int getSequenceOrder();
}