package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class InboundMemberSummaryTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "inbound_member_summary";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Inbound Membership & Dues Summary";
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
        return "Here is your active membership and payment summary at {{GYM.name}}:\n\n"
                + "📋 ACTIVE PLANS:\n"
                + "{{MEMBER.plans_breakdown}}\n\n"
                + "💳 PAYMENT STATUS:\n"
                + "• Total Paid: {{PAYMENT.total_paid}}\n"
                + "• Pending Balance: {{PAYMENT.pending_balance}}\n"
                + "• Next Due: {{PAYMENT.next_due_summary}}\n\n"
                + "Please visit reception or contact our front desk for renewals or dues settlement.";
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
        return Set.of("GYM.name", "MEMBER.plans_breakdown", "PAYMENT.total_paid", "PAYMENT.pending_balance", "PAYMENT.next_due_summary");
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.quickReplyWithFollowup("Main Menu", "INBOUND_WELCOME_MENU", InboundWelcomeTemplate.TEMPLATE_KEY, 1),
                TemplateButtonRecord.humanSupport("Chat with Reception", 2)
        );
    }
}
