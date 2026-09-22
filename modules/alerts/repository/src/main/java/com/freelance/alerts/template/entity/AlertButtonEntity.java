package com.freelance.alerts.template.entity;

import com.freelance.alerts.ButtonType;
import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "FL_ALERT_BUTTON")
@NamedQueries({
        @NamedQuery(
                name = "AlertButton.findByTemplateId",
                query = "SELECT b FROM AlertButtonEntity b WHERE b.template.id = :templateId ORDER BY b.sequenceOrder ASC"
        ),
        @NamedQuery(
                name = "AlertButton.findByPayload",
                query = "SELECT b FROM AlertButtonEntity b WHERE b.buttonType = com.freelance.alerts.ButtonType.QUICK_REPLY AND b.payload = :payload"
        )
})
public class AlertButtonEntity extends AbstractAuditEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private AlertTemplateEntity template;

    @Column(name = "button_text", nullable = false)
    private String buttonText;

    @Enumerated(EnumType.STRING)
    @Column(name = "button_type", nullable = false, length = 50)
    private ButtonType buttonType;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @OneToOne(mappedBy = "button", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private AlertButtonReplyEntity reply;

    public AlertButtonEntity() {}

    public AlertButtonEntity(UUID id, AlertTemplateEntity template, String buttonText, ButtonType buttonType, String payload, int sequenceOrder) {
        this.id = id;
        this.template = template;
        this.buttonText = buttonText;
        this.buttonType = buttonType;
        this.payload = payload;
        this.sequenceOrder = sequenceOrder;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public AlertTemplateEntity getTemplate() { return template; }
    public void setTemplate(AlertTemplateEntity template) { this.template = template; }
    public String getButtonText() { return buttonText; }
    public void setButtonText(String buttonText) { this.buttonText = buttonText; }
    public ButtonType getButtonType() { return buttonType; }
    public void setButtonType(ButtonType buttonType) { this.buttonType = buttonType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public int getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(int sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    public AlertButtonReplyEntity getReply() { return reply; }
    public void setReply(AlertButtonReplyEntity reply) { this.reply = reply; }
}