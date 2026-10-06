package com.freelance.alerts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaDownloadResponseDTO(
        byte[] data,
        String mimeType,
        String fileName
) {}
