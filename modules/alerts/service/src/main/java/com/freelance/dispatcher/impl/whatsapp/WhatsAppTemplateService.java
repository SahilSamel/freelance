package com.freelance.dispatcher.impl.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freelance.ConfigurationEngine;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.keys.alerts.webhook.WebhookConfigKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhatsAppTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppTemplateService.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TemplateStatus defaultMockStatus;
    private final boolean forceLiveMode;

    public record TemplateSubmissionResult(
            boolean successful,
            String metaTemplateId,
            TemplateStatus status,
            String errorMessage
    ) {
        public static TemplateSubmissionResult success(String metaTemplateId, TemplateStatus status) {
            return new TemplateSubmissionResult(true, metaTemplateId, status, null);
        }

        public static TemplateSubmissionResult failure(String errorMessage) {
            return new TemplateSubmissionResult(false, null, null, errorMessage);
        }
    }

    public WhatsAppTemplateService() {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new ObjectMapper(),
                TemplateStatus.SUBMITTED,
                false
        );
    }

    public WhatsAppTemplateService(HttpClient httpClient) {
        this(httpClient, new ObjectMapper(), TemplateStatus.SUBMITTED, false);
    }

    public WhatsAppTemplateService(HttpClient httpClient, boolean forceLiveMode) {
        this(httpClient, new ObjectMapper(), TemplateStatus.SUBMITTED, forceLiveMode);
    }

    public WhatsAppTemplateService(HttpClient httpClient, TemplateStatus defaultMockStatus) {
        this(httpClient, new ObjectMapper(), defaultMockStatus, false);
    }

    public WhatsAppTemplateService(HttpClient httpClient, ObjectMapper objectMapper, TemplateStatus defaultMockStatus) {
        this(httpClient, objectMapper, defaultMockStatus, false);
    }

    public WhatsAppTemplateService(HttpClient httpClient, ObjectMapper objectMapper, TemplateStatus defaultMockStatus, boolean forceLiveMode) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.defaultMockStatus = defaultMockStatus != null ? defaultMockStatus : TemplateStatus.SUBMITTED;
        this.forceLiveMode = forceLiveMode;
    }

    public TemplateSubmissionResult submitTemplate(AlertTemplateEntity entity, List<TemplateButtonDTO> buttons) {
        if (entity == null) {
            return TemplateSubmissionResult.failure("Template entity cannot be null");
        }

        if (buttons != null && buttons.size() > 3) {
            String listMetaId = "local_list_" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
            logger.info("[WhatsAppTemplateService] Template '{}' has {} options (interactive list). Auto-approved locally as session list with metaId={}",
                    entity.getTemplateKey(), buttons.size(), listMetaId);
            return TemplateSubmissionResult.success(listMetaId, TemplateStatus.APPROVED);
        }

        String accessToken = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN);
        String businessAccountId = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_BUSINESS_ACCOUNT_ID);
        String baseUrl = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_API_BASE_URL);

        if (isMockMode(accessToken, businessAccountId)) {
            String mockMetaId = "meta_tpl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
            logger.info("[WhatsAppTemplateService] (Mock Mode) Template '{}' registered with mock metaId={}, status={}",
                    entity.getTemplateKey(), mockMetaId, defaultMockStatus);
            return TemplateSubmissionResult.success(mockMetaId, defaultMockStatus);
        }

        try {
            String jsonPayload = buildPayload(entity, buttons);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + businessAccountId + "/message_templates"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                String id = rootNode.has("id") ? rootNode.get("id").asText() : null;
                String statusStr = rootNode.has("status") ? rootNode.get("status").asText() : null;

                TemplateStatus status = "APPROVED".equalsIgnoreCase(statusStr)
                        ? TemplateStatus.APPROVED
                        : TemplateStatus.SUBMITTED;

                return TemplateSubmissionResult.success(id, status);
            } else {
                String errorMsg = extractErrorMessage(response.body(), response.statusCode());
                logger.error("[WhatsAppTemplateService] Meta template submission failed with status {}: {}",
                        response.statusCode(), errorMsg);
                return TemplateSubmissionResult.failure(errorMsg);
            }
        } catch (Exception e) {
            logger.error("[WhatsAppTemplateService] Exception during Meta template submission for key {}",
                    entity.getTemplateKey(), e);
            return TemplateSubmissionResult.failure("Meta API call failed: " + e.getMessage());
        }
    }

    public String buildPayload(AlertTemplateEntity entity, List<TemplateButtonDTO> buttons) {
        ObjectNode root = objectMapper.createObjectNode();

        String metaName = formatMetaTemplateName(entity.getTemplateKey());
        root.put("name", metaName);
        root.put("category", formatMetaCategory(entity.getCategory()));
        root.put("language", entity.getLanguage() != null && !entity.getLanguage().isBlank() ? entity.getLanguage() : "en_US");

        ArrayNode components = root.putArray("components");

        // 1. HEADER component (if mediaType specified)
        if (entity.getMediaType() != null && !entity.getMediaType().isBlank()) {
            ObjectNode headerNode = components.addObject();
            headerNode.put("type", "HEADER");
            headerNode.put("format", entity.getMediaType().toUpperCase(Locale.ROOT));
        }

        // 2. BODY component
        ObjectNode bodyNode = components.addObject();
        bodyNode.put("type", "BODY");
        BodyFormatResult bodyResult = formatBodyTextAndExamples(entity.getContent());
        bodyNode.put("text", bodyResult.formattedText());

        if (!bodyResult.exampleValues().isEmpty()) {
            ObjectNode exampleNode = bodyNode.putObject("example");
            ArrayNode bodyTextArray = exampleNode.putArray("body_text");
            ArrayNode sampleRow = bodyTextArray.addArray();
            for (String sample : bodyResult.exampleValues()) {
                sampleRow.add(sample);
            }
        }

        // 3. BUTTONS component
        if (buttons != null && !buttons.isEmpty()) {
            ObjectNode buttonsNode = components.addObject();
            buttonsNode.put("type", "BUTTONS");
            ArrayNode btnArray = buttonsNode.putArray("buttons");

            for (TemplateButtonDTO b : buttons) {
                ObjectNode bNode = btnArray.addObject();
                if (b.type() == ButtonType.QUICK_REPLY) {
                    bNode.put("type", "QUICK_REPLY");
                    bNode.put("text", b.text());
                } else if (b.type() == ButtonType.URL) {
                    bNode.put("type", "URL");
                    bNode.put("text", b.text());
                    bNode.put("url", b.payload() != null ? b.payload() : "https://example.com");
                } else if (b.type() == ButtonType.PHONE_NUMBER) {
                    bNode.put("type", "PHONE_NUMBER");
                    bNode.put("text", b.text());
                    bNode.put("phone_number", b.payload() != null ? b.payload() : "+10000000000");
                }
            }
        }

        return root.toString();
    }

    public record BodyFormatResult(String formattedText, List<String> exampleValues) {}

    public BodyFormatResult formatBodyTextAndExamples(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return new BodyFormatResult("", Collections.emptyList());
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(rawContent);
        Map<String, Integer> seenPlaceholders = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        int index = 1;

        while (matcher.find()) {
            String placeholderName = matcher.group(1).trim();
            int placeholderIndex;
            if (seenPlaceholders.containsKey(placeholderName)) {
                placeholderIndex = seenPlaceholders.get(placeholderName);
            } else {
                placeholderIndex = index++;
                seenPlaceholders.put(placeholderName, placeholderIndex);
            }
            matcher.appendReplacement(sb, "{{" + placeholderIndex + "}}");
        }
        matcher.appendTail(sb);

        List<String> exampleValues = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : seenPlaceholders.entrySet()) {
            String name = entry.getKey();
            String sample = name.matches("^\\d+$") ? "sample_" + name : name;
            exampleValues.add(sample);
        }

        return new BodyFormatResult(sb.toString(), exampleValues);
    }

    public String formatMetaTemplateName(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return "template_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        return templateKey.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private String formatMetaCategory(String category) {
        if (category == null || category.isBlank()) {
            return "UTILITY";
        }
        String upper = category.toUpperCase(Locale.ROOT).trim();
        if (upper.contains("MARKET")) {
            return "MARKETING";
        } else if (upper.contains("AUTH")) {
            return "AUTHENTICATION";
        }
        return "UTILITY";
    }

    private boolean isMockMode(String accessToken, String businessAccountId) {
        if (forceLiveMode) {
            return false;
        }
        return accessToken == null
                || accessToken.contains("placeholder")
                || accessToken.contains("default")
                || businessAccountId == null
                || businessAccountId.contains("placeholder");
    }

    private String extractErrorMessage(String responseBody, int statusCode) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                if (root.has("error") && root.get("error").has("message")) {
                    return root.get("error").get("message").asText();
                }
            } catch (Exception ignored) {
            }
            return responseBody;
        }
        return "HTTP error " + statusCode;
    }
}
