package com.freelance.alerts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaUploadResponseDTO(
        String mediaId,
        String mediaUrl,
        String fileName,
        String mimeType,
        long fileSize
) {}
