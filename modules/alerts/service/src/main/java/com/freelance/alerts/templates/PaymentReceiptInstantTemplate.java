package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.DispatchScope;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class PaymentReceiptInstantTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "payment_receipt_instant";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Payment Receipt";
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
        return "Hey {{USER.first_name}}, we've received your payment! ✅\n\n"
                + "Your official payment receipt is attached above for your records. Tap below if you have any billing questions—otherwise, have a great session!";
    }

    @Override
    public String getMediaUrl() {
        return null;
    }

    @Override
    public String getMediaType() {
        return "DOCUMENT";
    }

    @Override
    public AlertChannel getChannel() {
        return AlertChannel.WHATSAPP;
    }

    @Override
    public Set<String> getRequiredAttributes() {
        return Set.of("USER.first_name");
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.humanSupport("Chat with Reception", 1)
        );
    }

    @Override
    public com.freelance.alerts.template.DispatchScope getDispatchScope() {
        return DispatchScope.INDIVIDUAL;
    }
}
