package com.freelance.alerts.dto;

import java.util.List;

public record TemplateBatchSubmitResponseDTO(
        int totalEvaluated,
        int totalSubmitted,
        int totalFailed,
        List<TemplateSubmitResponseDTO> results
) {}
