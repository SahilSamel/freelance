package com.freelance.dispatcher.impl.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freelance.ConfigurationEngine;
import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.dispatcher.data.AlertDispatchRequest;
import com.freelance.dispatcher.data.AlertDispatchResult;
import com.freelance.dispatcher.IAlertDispatcher;
import com.freelance.alerts.whatsapp.WhatsAppUtility;
import com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode;
import com.freelance.template.utils.TemplateRenderUtility;
import com.freelance.event.model.EventPriority;
import com.freelance.keys.alerts.webhook.WebhookConfigKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freelance.alerts.template.entity.AlertButtonEntity;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WhatsAppDispatcherService implements IAlertDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppDispatcherService.class);

    private final HttpClient httpClient;
    private final AlertMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    /**
     * Legacy DispatchResult kept for backward compatibility with existing tests.
     */
    public record DispatchResult(
            boolean successful,
            String metaMessageId,
            int httpStatusCode,
            String rawResponse,
            String renderedContent
    ) {}

    public WhatsAppDispatcherService() {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new AlertMessageRepository(),
                new ObjectMapper()
        );
    }

    public WhatsAppDispatcherService(AlertMessageRepository messageRepository) {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                messageRepository,
                new ObjectMapper()
        );
    }

    public WhatsAppDispatcherService(
            HttpClient httpClient,
            AlertMessageRepository messageRepository
    ) {
        this(httpClient, messageRepository, new ObjectMapper());
    }

    public WhatsAppDispatcherService(
            HttpClient httpClient,
            AlertMessageRepository messageRepository,
            ObjectMapper objectMapper
    ) {
        this.httpClient = httpClient;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public AlertChannel getChannel() {
        return AlertChannel.WHATSAPP;
    }

    @Override
    public AlertDispatchResult dispatch(AlertDispatchRequest request) {
        if (WhatsAppUtility.isQueuePaused()) {
            throw new IllegalStateException("Outbound queue is currently paused due to Meta account/billing issue.");
        }

        String renderedText = request.renderedContent() != null
                ? request.renderedContent()
                : TemplateRenderUtility.render(request.template(), request.resolvedAttributes());

        String recipientPhone = request.recipientContact();
        String accessToken = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN);
        String phoneNumberId = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_PHONE_NUMBER_ID);
        String baseUrl = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_API_BASE_URL);

        if (accessToken == null || accessToken.isBlank()
                || accessToken.trim().equalsIgnoreCase("default")
                || accessToken.trim().equalsIgnoreCase("placeholder")
                || accessToken.trim().startsWith("default_placeholder")
                || accessToken.trim().startsWith("placeholder")
                || accessToken.trim().startsWith("mock_")) {
            String mockWamid = "wamid.HBgL" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            logger.info("[WhatsAppDispatcherService] (Mock Mode) Dispatched to {}: wamid={}", recipientPhone, mockWamid);
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.putArray("messages").addObject().put("id", mockWamid);
            return AlertDispatchResult.success(mockWamid, mockResponse.toString());
        }

        try {
            String jsonPayload;
            if (request.template() != null && request.template().getChannel() == AlertChannel.WHATSAPP) {
                List<AlertButtonEntity> buttons = request.template().getButtons();
                if (buttons != null && buttons.size() > 3) {
                    jsonPayload = buildInteractiveListPayload(recipientPhone, renderedText, buttons);
                } else {
                    jsonPayload = buildTemplatePayload(recipientPhone, request);
                }
            } else if (request.mediaId() != null || request.mediaUrl() != null) {
                jsonPayload = buildDirectMediaPayload(recipientPhone, request, renderedText);
            } else {
                jsonPayload = buildTextPayload(recipientPhone, renderedText);
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + phoneNumberId + "/messages"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String wamid = extractWamid(response.body());
                return AlertDispatchResult.success(wamid, response.body());
            } else {
                return AlertDispatchResult.failure(
                        WhatsAppErrorCode.CODE_DISPATCH_FAILED,
                        "Meta WhatsApp API HTTP " + response.statusCode(),
                        response.body()
                );
            }
        } catch (Exception e) {
            logger.error("[WhatsAppDispatcherService] Dispatch HTTP call failed to recipient {}", recipientPhone, e);
            return AlertDispatchResult.failure(WhatsAppErrorCode.CODE_DISPATCH_FAILED, e.getMessage(), e.toString());
        }
    }

    private String buildTemplatePayload(String recipientPhone, AlertDispatchRequest request) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("messaging_product", "whatsapp");
        root.put("recipient_type", "individual");
        root.put("to", cleanPhone(recipientPhone));
        root.put("type", "template");

        ObjectNode templateNode = root.putObject("template");
        templateNode.put("name", request.template().getTemplateKey().toLowerCase());
        String langCode = request.template().getLanguage() != null ? request.template().getLanguage() : "en_US";
        templateNode.putObject("language").put("code", langCode);

        String mediaUrl = request.template().getMediaUrl() != null && !request.template().getMediaUrl().isBlank()
                ? request.template().getMediaUrl()
                : request.mediaUrl();

        if (mediaUrl != null && !mediaUrl.isBlank()) {
            String mediaParamType = "DOCUMENT".equalsIgnoreCase(request.template().getMediaType()) || mediaUrl.toLowerCase().endsWith(".pdf")
                    ? "document"
                    : "image";

            ArrayNode components = templateNode.putArray("components");
            ObjectNode header = components.addObject();
            header.put("type", "header");
            ArrayNode parameters = header.putArray("parameters");
            ObjectNode param = parameters.addObject();
            param.put("type", mediaParamType);
            param.putObject(mediaParamType).put("link", mediaUrl);
        }

        return objectMapper.writeValueAsString(root);
    }

    private String buildDirectMediaPayload(String recipientPhone, AlertDispatchRequest request, String renderedText) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("messaging_product", "whatsapp");
        root.put("recipient_type", "individual");
        root.put("to", cleanPhone(recipientPhone));

        boolean isDoc = "DOCUMENT".equalsIgnoreCase(request.mediaType())
                || (request.mediaUrl() != null && request.mediaUrl().toLowerCase().endsWith(".pdf"));
        String mediaField = isDoc ? "document" : "image";
        root.put("type", mediaField);

        ObjectNode mediaNode = root.putObject(mediaField);
        if (request.mediaId() != null && !request.mediaId().isBlank()) {
            mediaNode.put("id", request.mediaId());
        } else {
            mediaNode.put("link", request.mediaUrl());
        }

        if (renderedText != null && !renderedText.equals("[Image]") && !renderedText.equals("[Document]") && !renderedText.isBlank()) {
            mediaNode.put("caption", renderedText);
        }

        return objectMapper.writeValueAsString(root);
    }

    private String buildTextPayload(String recipientPhone, String renderedText) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("messaging_product", "whatsapp");
        root.put("recipient_type", "individual");
        root.put("to", cleanPhone(recipientPhone));
        root.put("type", "text");

        ObjectNode textNode = root.putObject("text");
        textNode.put("preview_url", false);
        textNode.put("body", renderedText != null ? renderedText : "");

        return objectMapper.writeValueAsString(root);
    }

    private String cleanPhone(String phone) {
        return phone != null ? phone.replace("+", "").trim() : "";
    }

    /**
     * Backward-compatible convenience method for legacy callers.
     */
    public DispatchResult dispatch(
            AlertTemplateEntity template,
            String recipientPhone,
            Map<String, String> resolvedAttributes
    ) {
        String renderedText = TemplateRenderUtility.render(template, resolvedAttributes);
        AlertDispatchRequest request = new AlertDispatchRequest(
                null,
                template,
                AlertChannel.WHATSAPP,
                recipientPhone,
                renderedText,
                resolvedAttributes,
                EventPriority.NORMAL
        );
        AlertDispatchResult res = dispatch(request);
        return new DispatchResult(
                res.isSuccess(),
                res.externalMessageId(),
                res.isSuccess() ? 200 : 500,
                res.rawResponse(),
                renderedText
        );
    }

    public String renderContent(String templateContent, Map<String, String> resolvedAttributes) {
        return TemplateRenderUtility.render(templateContent, resolvedAttributes);
    }

    private String extractWamid(String json) {
        if (json != null && !json.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode messagesNode = root.path("messages");
                if (messagesNode.isArray() && !messagesNode.isEmpty()) {
                    JsonNode firstMsg = messagesNode.get(0);
                    if (firstMsg.hasNonNull("id")) {
                        return firstMsg.get("id").asText();
                    }
                }
                if (root.hasNonNull("id")) {
                    return root.get("id").asText();
                }
            } catch (Exception e) {
                logger.warn("[WhatsAppDispatcherService] Could not parse WAMID from response JSON: {}", json, e);
            }
        }
        return "wamid.HBgL" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public String buildInteractiveListPayload(String recipientPhone, String bodyText, List<AlertButtonEntity> buttons) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("messaging_product", "whatsapp");
        root.put("recipient_type", "individual");
        root.put("to", cleanPhone(recipientPhone));
        root.put("type", "interactive");

        ObjectNode interactive = root.putObject("interactive");
        interactive.put("type", "list");
        interactive.putObject("body").put("text", (bodyText != null && !bodyText.isBlank()) ? bodyText : "Please select an option:");

        ObjectNode action = interactive.putObject("action");
        action.put("button", "Select Option");
        ArrayNode sections = action.putArray("sections");
        ObjectNode section = sections.addObject();
        section.put("title", "Options");
        ArrayNode rows = section.putArray("rows");

        if (buttons != null) {
            for (int i = 0; i < buttons.size(); i++) {
                AlertButtonEntity b = buttons.get(i);
                ObjectNode row = rows.addObject();
                row.put("id", b.getPayload() != null ? b.getPayload() : "opt_" + b.getSequenceOrder());
                row.put("title", b.getButtonText() != null ? b.getButtonText() : "Option " + (i + 1));
            }
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize interactive list payload", e);
        }
    }
}
