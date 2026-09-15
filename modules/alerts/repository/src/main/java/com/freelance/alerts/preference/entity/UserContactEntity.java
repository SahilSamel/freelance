package com.freelance.alerts.preference.entity;

import com.freelance.alerts.AlertChannel;
import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "FL_USER_CONTACT", schema = "freelance")
@NamedQueries({
        @NamedQuery(
                name = "UserContact.findByUserAndChannel",
                query = "SELECT c FROM UserContactEntity c WHERE c.userId = :userId AND c.channel = :channel"
        ),
        @NamedQuery(
                name = "UserContact.findByContactAndChannel",
                query = "SELECT c FROM UserContactEntity c WHERE c.contactValue = :contactValue AND c.channel = :channel"
        ),
        @NamedQuery(
                name = "UserContact.findByUserId",
                query = "SELECT c FROM UserContactEntity c WHERE c.userId = :userId"
        )
})
public class UserContactEntity extends AbstractAuditEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private AlertChannel channel;

    @Column(name = "contact_value", nullable = false)
    private String contactValue;

    @Column(name = "is_opted_out", nullable = false)
    private boolean isOptedOut;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "human_session_expires_at")
    private Instant humanSessionExpiresAt;

    public UserContactEntity() {}

    public UserContactEntity(UUID id, UUID userId, AlertChannel channel, String contactValue) {
        this(id, userId, channel, contactValue, false, true, null);
    }

    public UserContactEntity(UUID id, UUID userId, AlertChannel channel, String contactValue, boolean isOptedOut, boolean isActive) {
        this(id, userId, channel, contactValue, isOptedOut, isActive, null);
    }

    public UserContactEntity(UUID id, UUID userId, AlertChannel channel, String contactValue, boolean isOptedOut, boolean isActive, Instant humanSessionExpiresAt) {
        this.id = id;
        this.userId = userId;
        this.channel = channel;
        this.contactValue = contactValue;
        this.isOptedOut = isOptedOut;
        this.isActive = isActive;
        this.humanSessionExpiresAt = humanSessionExpiresAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public AlertChannel getChannel() { return channel; }
    public void setChannel(AlertChannel channel) { this.channel = channel; }
    public String getContactValue() { return contactValue; }
    public void setContactValue(String contactValue) { this.contactValue = contactValue; }
    public boolean isOptedOut() { return isOptedOut; }
    public void setOptedOut(boolean optedOut) { isOptedOut = optedOut; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Instant getHumanSessionExpiresAt() { return humanSessionExpiresAt; }
    public void setHumanSessionExpiresAt(Instant humanSessionExpiresAt) { this.humanSessionExpiresAt = humanSessionExpiresAt; }

    public boolean isHumanSessionActive() {
        return humanSessionExpiresAt != null && humanSessionExpiresAt.isAfter(Instant.now());
    }
}