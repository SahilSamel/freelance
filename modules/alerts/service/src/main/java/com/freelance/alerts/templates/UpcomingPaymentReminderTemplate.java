package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class UpcomingPaymentReminderTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "upcoming_payment_reminder";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Upcoming Payment Reminder";
    }

    @Override
    public String getCategory() {
        return TemplateCategory.UTILITY.name();
    }

    @Override
    public String getLanguage() {
        return "en";
    }

    @Override
    public String getContent() {
        return "Hey {{USER.first_name}}, a quick heads-up from *{{GYM.name}}*! ⏳\n\n"
                + "Your next membership installment is coming up soon:\n\n"
                + "💳 *Amount Due:* {{PAYMENT.amount}}\n"
                + "📅 *Due Date:* {{PAYMENT.due_date}} ({{PAYMENT.days_left}} days left)\n\n"
                + "You can scan the UPI QR code above (or visit {{GYM.payment_qr}}) to clear this instantly, or settle it at the front desk on your next visit.\n\n"
                + "Tap below to ask reception for payment options or share your payment confirmation!";
    }

    @Override
    public String getMediaUrl() {
        return null;
    }

    @Override
    public String getMediaType() {
        return null;
    }

    @Override
    public AlertChannel getChannel() {
        return AlertChannel.WHATSAPP;
    }

    @Override
    public Set<String> getRequiredAttributes() {
        return Set.of(
                "USER.first_name",
                "GYM.name",
                "GYM.payment_qr",
                "PAYMENT.amount",
                "PAYMENT.due_date",
                "PAYMENT.days_left"
        );
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.humanSupport("Chat with Reception", 1)
        );
    }
}
