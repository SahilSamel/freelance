package com.freelance.alerts.channel.repository;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.channel.entity.AlertChannelConfigEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.Optional;

public class AlertChannelConfigRepository extends AbstractRepository<AlertChannelConfigEntity, AlertChannel> {

    public Optional<AlertChannelConfigEntity> findByChannel(AlertChannel channel) {
        if (channel == null) return Optional.empty();
        return Optional.ofNullable(get(channel));
    }

    public boolean isChannelAvailable(AlertChannel channel) {
        if (channel == null) return false;
        AlertChannelConfigEntity config = get(channel);
        if (config == null) {
            // Default to available if unconfigured
            return true;
        }
        return config.isActive();
    }
}
