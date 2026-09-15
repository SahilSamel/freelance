package com.freelance.alerts.channel.entity;

import com.freelance.alerts.AlertChannel;
import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "FL_ALERT_CHANNEL_CONFIG", schema = "freelance")
public class AlertChannelConfigEntity extends AbstractAuditEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private AlertChannel channel;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "pause_reason", length = 255)
    private String pauseReason;

    public AlertChannelConfigEntity() {}

    public AlertChannelConfigEntity(AlertChannel channel, boolean isActive, String pauseReason) {
        this.channel = channel;
        this.isActive = isActive;
        this.pauseReason = pauseReason;
    }

    public AlertChannel getChannel() { return channel; }
    public void setChannel(AlertChannel channel) { this.channel = channel; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public String getPauseReason() { return pauseReason; }
    public void setPauseReason(String pauseReason) { this.pauseReason = pauseReason; }
}
