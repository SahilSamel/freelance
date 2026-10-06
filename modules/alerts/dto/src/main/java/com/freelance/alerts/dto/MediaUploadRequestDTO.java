package com.freelance.alerts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaUploadRequestDTO(
        String fileName,
        String mimeType,
        String dataBase64
) {}
