package com.freelance.alerts.template.entity;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "FL_ALERT_BUTTON_REPLY", schema = "freelance")
@NamedQueries({
        @NamedQuery(
                name = "AlertButtonReply.findByButtonId",
                query = "SELECT r FROM AlertButtonReplyEntity r WHERE r.button.id = :buttonId"
        ),
        @NamedQuery(
                name = "AlertButtonReply.findByButtonPayload",
                query = "SELECT r FROM AlertButtonReplyEntity r WHERE r.button.buttonType = com.freelance.alerts.ButtonType.QUICK_REPLY AND r.button.payload = :payload"
        )
})
public class AlertButtonReplyEntity extends AbstractAuditEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "button_id", nullable = false, unique = true)
    private AlertButtonEntity button;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reply_template_id")
    private AlertTemplateEntity replyTemplate;

    @Column(name = "requires_human_agent", nullable = false)
    private boolean requiresHumanAgent;

    public AlertButtonReplyEntity() {}

    public AlertButtonReplyEntity(UUID id, AlertButtonEntity button, AlertTemplateEntity replyTemplate, boolean requiresHumanAgent) {
        this.id = id;
        this.button = button;
        this.replyTemplate = replyTemplate;
        this.requiresHumanAgent = requiresHumanAgent;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public AlertButtonEntity getButton() { return button; }
    public void setButton(AlertButtonEntity button) { this.button = button; }
    public AlertTemplateEntity getReplyTemplate() { return replyTemplate; }
    public void setReplyTemplate(AlertTemplateEntity replyTemplate) { this.replyTemplate = replyTemplate; }
    public boolean isRequiresHumanAgent() { return requiresHumanAgent; }
    public void setRequiresHumanAgent(boolean requiresHumanAgent) { this.requiresHumanAgent = requiresHumanAgent; }
}