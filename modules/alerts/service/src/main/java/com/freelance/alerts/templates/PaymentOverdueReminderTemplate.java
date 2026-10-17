package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class PaymentOverdueReminderTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "payment_overdue_reminder";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Payment Overdue Reminder";
    }

    @Override
    public String getCategory() {
        return TemplateCategory.UTILITY.name();
    }

    @Override
    public String getLanguage() {
        return "en_US";
    }

    @Override
    public String getContent() {
        return "Hey {{USER.first_name}}, a quick heads-up regarding your pending membership balance at *{{GYM.name}}*.\n\n"
                + "*Pending Breakdown:*\n"
                + "{{PAYMENT.overdue_breakdown}}\n\n"
                + "💳 *Total Due:* {{PAYMENT.pending_balance}}\n"
                + "📅 *Due Since:* {{PAYMENT.overdue_since}}\n\n"
                + "You can scan the UPI QR code above (or visit {{GYM.payment_qr}}) to clear this instantly, or settle it at the front desk on your next visit.\n\n"
                + "Already paid or have a question? Tap below to let our reception know!";
    }

    @Override
    public String getMediaUrl() {
        return "/alerts/media/med_upi_qr_code";
    }

    @Override
    public String getMediaType() {
        return "IMAGE";
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
                "PAYMENT.overdue_breakdown",
                "PAYMENT.pending_balance",
                "PAYMENT.overdue_since"
        );
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.humanSupport("Chat with Reception", 1)
        );
    }
}
