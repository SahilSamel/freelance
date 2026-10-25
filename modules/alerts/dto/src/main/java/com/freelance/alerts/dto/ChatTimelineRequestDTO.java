package com.freelance.alerts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.freelance.alerts.AlertChannel;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatTimelineRequestDTO(
        @JsonProperty("userId") UUID userId,
        @JsonProperty("channel") AlertChannel channel,
        @JsonProperty("limit") Integer limit,
        @JsonProperty("unreadOnly") Boolean unreadOnly,
        @JsonProperty("beforeTimestamp") Instant beforeTimestamp
) {
    public ChatTimelineRequestDTO(UUID userId, AlertChannel channel, Integer limit) {
        this(userId, channel, limit, false, null);
    }

    public ChatTimelineRequestDTO(UUID userId, AlertChannel channel, Integer limit, Boolean unreadOnly) {
        this(userId, channel, limit, unreadOnly, null);
    }
}
