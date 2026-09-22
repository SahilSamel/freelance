package com.freelance.alerts.message.entity;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "FL_ALERT_MESSAGE")
@NamedQueries({
        @NamedQuery(
                name = "AlertMessage.findByUserId",
                query = "SELECT m FROM AlertMessageEntity m WHERE m.userId = :userId ORDER BY m.timestamp ASC, m.createdDate ASC"
        ),
        @NamedQuery(
                name = "AlertMessage.findByUserAndChannel",
                query = "SELECT m FROM AlertMessageEntity m WHERE m.userId = :userId AND m.channel = :channel ORDER BY m.timestamp ASC, m.createdDate ASC"
        ),
        @NamedQuery(
                name = "AlertMessage.findByMetaMessageId",
                query = "SELECT m FROM AlertMessageEntity m WHERE m.metaMessageId = :metaMessageId"
        ),
        @NamedQuery(
                name = "AlertMessage.findLatestByUserId",
                query = "SELECT m FROM AlertMessageEntity m WHERE m.userId = :userId ORDER BY m.timestamp DESC"
        ),
        @NamedQuery(
                name = "AlertMessage.findLatestByUserAndChannel",
                query = "SELECT m FROM AlertMessageEntity m WHERE m.userId = :userId AND m.channel = :channel ORDER BY m.timestamp DESC"
        ),
        @NamedQuery(
                name = "AlertMessage.findUnreadByUserAndChannel",
                query = "SELECT m FROM AlertMessageEntity m WHERE m.userId = :userId AND m.channel = :channel AND m.status != com.freelance.alerts.AlertMessageStatus.READ ORDER BY m.timestamp ASC, m.createdDate ASC"
        ),
        @NamedQuery(
                name = "AlertMessage.findUnreadByUserId",
                query = "SELECT m FROM AlertMessageEntity m WHERE m.userId = :userId AND m.status != com.freelance.alerts.AlertMessageStatus.READ ORDER BY m.timestamp ASC, m.createdDate ASC"
        )
})
public class AlertMessageEntity extends AbstractAuditEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private AlertChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private AlertDirection direction;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id")
    private AlertTemplateEntity template;

    @Column(name = "meta_message_id")
    private String metaMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertMessageStatus status;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "raw_error_details", columnDefinition = "TEXT")
    private String rawErrorDetails;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public AlertMessageEntity() {}

    public AlertMessageEntity(
            UUID id,
            UUID userId,
            AlertChannel channel,
            AlertDirection direction,
            String messageType,
            String content,
            String metaMessageId,
            AlertMessageStatus status,
            String errorCode,
            String rawErrorDetails,
            Instant timestamp
    ) {
        this(id, userId, channel, direction, messageType, content, null, metaMessageId, status, errorCode, rawErrorDetails, timestamp);
    }

    public AlertMessageEntity(
            UUID id,
            UUID userId,
            AlertChannel channel,
            AlertDirection direction,
            String messageType,
            String content,
            AlertTemplateEntity template,
            String metaMessageId,
            AlertMessageStatus status,
            String errorCode,
            String rawErrorDetails,
            Instant timestamp
    ) {
        this.id = id;
        this.userId = userId;
        this.channel = channel;
        this.direction = direction;
        this.messageType = messageType;
        this.content = content;
        this.template = template;
        this.metaMessageId = metaMessageId;
        this.status = status;
        this.errorCode = errorCode;
        this.rawErrorDetails = rawErrorDetails;
        this.timestamp = timestamp;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public AlertChannel getChannel() { return channel; }
    public void setChannel(AlertChannel channel) { this.channel = channel; }
    public AlertDirection getDirection() { return direction; }
    public void setDirection(AlertDirection direction) { this.direction = direction; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public AlertTemplateEntity getTemplate() { return template; }
    public void setTemplate(AlertTemplateEntity template) { this.template = template; }
    public String getMetaMessageId() { return metaMessageId; }
    public void setMetaMessageId(String metaMessageId) { this.metaMessageId = metaMessageId; }
    public AlertMessageStatus getStatus() { return status; }
    public void setStatus(AlertMessageStatus status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getRawErrorDetails() { return rawErrorDetails; }
    public void setRawErrorDetails(String rawErrorDetails) { this.rawErrorDetails = rawErrorDetails; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}