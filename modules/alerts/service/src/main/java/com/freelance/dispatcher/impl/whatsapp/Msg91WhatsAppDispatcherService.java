package com.freelance.dispatcher.impl.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freelance.ConfigurationEngine;
import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.whatsapp.WhatsAppUtility;
import com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode;
import com.freelance.dispatcher.IAlertDispatcher;
import com.freelance.dispatcher.data.AlertDispatchRequest;
import com.freelance.dispatcher.data.AlertDispatchResult;
import com.freelance.keys.alerts.webhook.WebhookConfigKeys;
import com.freelance.template.utils.TemplateRenderUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dispatcher implementation for MSG91 WhatsApp Cloud API (v5).
 * Supports pre-approved templates, dynamic body variables, header media,
 * interactive button components, and direct fallback messages.
 */
public class Msg91WhatsAppDispatcherService implements IAlertDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(Msg91WhatsAppDispatcherService.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private final HttpClient httpClient;
    private final AlertMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public Msg91WhatsAppDispatcherService() {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new AlertMessageRepository(),
                new ObjectMapper()
        );
    }

    public Msg91WhatsAppDispatcherService(AlertMessageRepository messageRepository) {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                messageRepository,
                new ObjectMapper()
        );
    }

    public Msg91WhatsAppDispatcherService(
            HttpClient httpClient,
            AlertMessageRepository messageRepository,
            ObjectMapper objectMapper
    ) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.messageRepository = messageRepository != null ? messageRepository : new AlertMessageRepository();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public AlertChannel getChannel() {
        return AlertChannel.WHATSAPP;
    }

    @Override
    public AlertDispatchResult dispatch(AlertDispatchRequest request) {
        if (WhatsAppUtility.isQueuePaused()) {
            throw new IllegalStateException("Outbound queue is currently paused due to Meta/MSG91 account issue.");
        }

        String renderedText = request.renderedContent() != null
                ? request.renderedContent()
                : TemplateRenderUtility.render(request.template(), request.resolvedAttributes());

        String recipientPhone = cleanPhone(request.recipientContact());
        String authKey = ConfigurationEngine.get(WebhookConfigKeys.MSG91_AUTH_KEY);
        String integratedNumber = cleanPhone(ConfigurationEngine.get(WebhookConfigKeys.MSG91_INTEGRATED_NUMBER));
        String baseUrl = ConfigurationEngine.get(WebhookConfigKeys.MSG91_API_BASE_URL);
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.msg91.com/api/v5/whatsapp";
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        if (isMockMode(authKey)) {
            String mockRequestId = "msg91_req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            logger.info("[Msg91WhatsAppDispatcherService] (Mock Mode) Dispatched to {}: requestId={}", recipientPhone, mockRequestId);
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("status", "success");
            mockResponse.put("message", "Message sent successfully (mock mode)");
            mockResponse.put("request_id", mockRequestId);
            return AlertDispatchResult.success(mockRequestId, mockResponse.toString());
        }

        try {
            String jsonPayload;
            if (request.template() != null && request.template().getChannel() == AlertChannel.WHATSAPP) {
                jsonPayload = buildTemplatePayload(integratedNumber, recipientPhone, request);
            } else if (request.mediaId() != null || request.mediaUrl() != null) {
                jsonPayload = buildMediaPayload(integratedNumber, recipientPhone, request, renderedText);
            } else {
                jsonPayload = buildTextPayload(integratedNumber, recipientPhone, renderedText);
            }

            URI targetUri = URI.create(baseUrl + "/whatsapp-outbound-message/");

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .header("authkey", authKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String requestId = extractRequestId(response.body());
                return AlertDispatchResult.success(requestId, response.body());
            } else {
                logger.error("[Msg91WhatsAppDispatcherService] MSG91 HTTP {}: {}", response.statusCode(), response.body());
                return AlertDispatchResult.failure(
                        WhatsAppErrorCode.CODE_DISPATCH_FAILED,
                        "MSG91 API HTTP " + response.statusCode(),
                        response.body()
                );
            }
        } catch (Exception e) {
            logger.error("[Msg91WhatsAppDispatcherService] Dispatch HTTP call failed to recipient {}", recipientPhone, e);
            return AlertDispatchResult.failure(WhatsAppErrorCode.CODE_DISPATCH_FAILED, e.getMessage(), e.toString());
        }
    }

    private String buildTemplatePayload(String integratedNumber, String recipientPhone, AlertDispatchRequest request) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("integrated_number", integratedNumber);
        root.put("content_type", "template");

        ObjectNode payload = root.putObject("payload");
        payload.put("to", recipientPhone);
        payload.put("type", "template");

        ObjectNode templateNode = payload.putObject("template");
        String tplName = request.template().getTemplateKey().toLowerCase();
        templateNode.put("name", tplName);

        String langCode = request.template().getLanguage() != null && !request.template().getLanguage().isBlank()
                ? request.template().getLanguage()
                : "en";
        templateNode.putObject("language")
                .put("code", langCode)
                .put("policy", "deterministic");

        ArrayNode toAndComponents = templateNode.putArray("to_and_components");
        ObjectNode recipientComponent = toAndComponents.addObject();
        recipientComponent.putArray("to").add(recipientPhone);

        ObjectNode components = recipientComponent.putObject("components");

        // 1. Header component (Media)
        String mediaUrl = request.template().getMediaUrl() != null && !request.template().getMediaUrl().isBlank()
                ? request.template().getMediaUrl()
                : request.mediaUrl();

        if (mediaUrl != null && !mediaUrl.isBlank()) {
            boolean isDoc = "DOCUMENT".equalsIgnoreCase(request.template().getMediaType()) || mediaUrl.toLowerCase().endsWith(".pdf");
            ObjectNode header = components.putObject("header_1");
            header.put("type", isDoc ? "document" : "image");
            header.put("value", mediaUrl);
        }

        // 2. Body parameters from resolvedAttributes or sequential placeholders
        Map<String, String> attributes = request.resolvedAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            int bodyIndex = 1;
            // If template content has {{placeholder}}, order them accordingly
            String content = request.template().getContent();
            if (content != null && !content.isBlank()) {
                Matcher matcher = VARIABLE_PATTERN.matcher(content);
                while (matcher.find()) {
                    String rawVar = matcher.group(1).trim();
                    String val = attributes.getOrDefault(rawVar, attributes.getOrDefault(rawVar.toLowerCase(), ""));
                    ObjectNode bodyParam = components.putObject("body_" + bodyIndex);
                    bodyParam.put("type", "text");
                    bodyParam.put("value", val != null ? val : "");
                    bodyIndex++;
                }
            } else {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    ObjectNode bodyParam = components.putObject("body_" + bodyIndex);
                    bodyParam.put("type", "text");
                    bodyParam.put("value", entry.getValue() != null ? entry.getValue() : "");
                    bodyIndex++;
                }
            }
        }

        // 3. Button components (if any dynamic payload buttons)
        List<AlertButtonEntity> buttons = request.template().getButtons();
        if (buttons != null && !buttons.isEmpty()) {
            for (int i = 0; i < buttons.size(); i++) {
                AlertButtonEntity b = buttons.get(i);
                if (b.getPayload() != null && !b.getPayload().isBlank()) {
                    ObjectNode btn = components.putObject("button_" + (i + 1));
                    btn.put("subtype", "url");
                    btn.put("type", "text");
                    btn.put("value", b.getPayload());
                }
            }
        }

        return objectMapper.writeValueAsString(root);
    }

    private String buildMediaPayload(String integratedNumber, String recipientPhone, AlertDispatchRequest request, String renderedText) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("integrated_number", integratedNumber);

        boolean isDoc = "DOCUMENT".equalsIgnoreCase(request.mediaType())
                || (request.mediaUrl() != null && request.mediaUrl().toLowerCase().endsWith(".pdf"));
        String mediaType = isDoc ? "document" : "image";
        root.put("content_type", mediaType);

        ObjectNode payload = root.putObject("payload");
        payload.put("to", recipientPhone);
        payload.put("type", mediaType);

        ObjectNode mediaNode = payload.putObject(mediaType);
        if (request.mediaUrl() != null && !request.mediaUrl().isBlank()) {
            mediaNode.put("link", request.mediaUrl());
        } else if (request.mediaId() != null && !request.mediaId().isBlank()) {
            mediaNode.put("id", request.mediaId());
        }

        if (renderedText != null && !renderedText.equals("[Image]") && !renderedText.equals("[Document]") && !renderedText.isBlank()) {
            mediaNode.put("caption", renderedText);
        }

        return objectMapper.writeValueAsString(root);
    }

    private String buildTextPayload(String integratedNumber, String recipientPhone, String renderedText) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("integrated_number", integratedNumber);
        root.put("content_type", "text");

        ObjectNode payload = root.putObject("payload");
        payload.put("to", recipientPhone);
        payload.put("type", "text");

        ObjectNode textNode = payload.putObject("text");
        textNode.put("body", renderedText != null ? renderedText : "");

        return objectMapper.writeValueAsString(root);
    }

    private String cleanPhone(String phone) {
        if (phone == null) return "";
        return phone.replace("+", "").replaceAll("[^0-9]", "").trim();
    }

    private boolean isMockMode(String authKey) {
        if (authKey == null || authKey.isBlank()) {
            return true;
        }
        String trimmed = authKey.trim().toLowerCase();
        return trimmed.equals("default")
                || trimmed.equals("placeholder")
                || trimmed.startsWith("default_placeholder")
                || trimmed.startsWith("placeholder")
                || trimmed.startsWith("mock_");
    }

    private String extractRequestId(String json) {
        if (json != null && !json.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(json);
                if (root.hasNonNull("request_id")) {
                    return root.get("request_id").asText();
                }
                if (root.hasNonNull("requestId")) {
                    return root.get("requestId").asText();
                }
                if (root.hasNonNull("msg_id")) {
                    return root.get("msg_id").asText();
                }
                if (root.hasNonNull("id")) {
                    return root.get("id").asText();
                }
                if (root.has("data") && root.get("data").hasNonNull("request_id")) {
                    return root.get("data").get("request_id").asText();
                }
            } catch (Exception e) {
                logger.warn("[Msg91WhatsAppDispatcherService] Could not parse requestId from response JSON: {}", json, e);
            }
        }
        return "msg91_req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
