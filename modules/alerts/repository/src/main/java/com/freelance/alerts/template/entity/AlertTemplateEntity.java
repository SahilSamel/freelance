package com.freelance.alerts.template.entity;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "FL_ALERT_TEMPLATE")
@NamedQueries({
        @NamedQuery(
                name = "AlertTemplate.findByKey",
                query = "SELECT t FROM AlertTemplateEntity t WHERE t.templateKey = :templateKey"
        ),
        @NamedQuery(
                name = "AlertTemplate.findAll",
                query = "SELECT t FROM AlertTemplateEntity t ORDER BY t.templateKey ASC"
        ),
        @NamedQuery(
                name = "AlertTemplate.findByStatus",
                query = "SELECT t FROM AlertTemplateEntity t WHERE t.status = :status ORDER BY t.templateKey ASC"
        ),
        @NamedQuery(
                name = "AlertTemplate.findByKeyIgnoreCase",
                query = "SELECT t FROM AlertTemplateEntity t WHERE LOWER(t.templateKey) = LOWER(:templateKey)"
        ),
        @NamedQuery(
                name = "AlertTemplate.findByMetaTemplateId",
                query = "SELECT t FROM AlertTemplateEntity t WHERE t.metaTemplateId = :metaTemplateId"
        )
})
public class AlertTemplateEntity extends AbstractAuditEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "template_key", nullable = false, unique = true)
    private String templateKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "media_type", length = 50)
    private String mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private AlertChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TemplateStatus status;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "required_attributes", columnDefinition = "TEXT")
    private String requiredAttributes;

    @Column(name = "meta_template_id")
    private String metaTemplateId;

    @Column(name = "priority", length = 20)
    private String priority;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<AlertButtonEntity> buttons = new ArrayList<>();

    public AlertTemplateEntity() {}

    public AlertTemplateEntity(
            UUID id,
            String templateKey,
            String name,
            String category,
            String language,
            String content,
            String mediaUrl,
            String mediaType,
            AlertChannel channel,
            TemplateStatus status,
            boolean isSystem,
            String requiredAttributes,
            String metaTemplateId
    ) {
        this(id, templateKey, name, category, language, content, mediaUrl, mediaType, channel, status, isSystem, requiredAttributes, metaTemplateId, null);
    }

    public AlertTemplateEntity(
            UUID id,
            String templateKey,
            String name,
            String category,
            String language,
            String content,
            String mediaUrl,
            String mediaType,
            AlertChannel channel,
            TemplateStatus status,
            boolean isSystem,
            String requiredAttributes,
            String metaTemplateId,
            String priority
    ) {
        this.id = id;
        this.templateKey = templateKey;
        this.name = name;
        this.category = category;
        this.language = language;
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.channel = channel;
        this.status = status;
        this.isSystem = isSystem;
        this.requiredAttributes = requiredAttributes;
        this.metaTemplateId = metaTemplateId;
        this.priority = priority;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public AlertChannel getChannel() { return channel; }
    public void setChannel(AlertChannel channel) { this.channel = channel; }
    public TemplateStatus getStatus() { return status; }
    public void setStatus(TemplateStatus status) { this.status = status; }
    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }
    public String getRequiredAttributes() { return requiredAttributes; }
    public void setRequiredAttributes(String requiredAttributes) { this.requiredAttributes = requiredAttributes; }
    public String getMetaTemplateId() { return metaTemplateId; }
    public void setMetaTemplateId(String metaTemplateId) { this.metaTemplateId = metaTemplateId; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public List<AlertButtonEntity> getButtons() { return buttons; }
    public void setButtons(List<AlertButtonEntity> buttons) { this.buttons = buttons; }
}