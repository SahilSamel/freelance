package com.freelance.dispatcher.impl.whatsapp;

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
                new AlertMessageRepository()
        );
    }

    public WhatsAppDispatcherService(AlertMessageRepository messageRepository) {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                messageRepository
        );
    }

    public WhatsAppDispatcherService(
            HttpClient httpClient,
            AlertMessageRepository messageRepository
    ) {
        this.httpClient = httpClient;
        this.messageRepository = messageRepository;
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

        if (accessToken == null || accessToken.contains("placeholder") || accessToken.contains("default")) {
            String mockWamid = "wamid.HBgL" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            logger.info("[WhatsAppDispatcherService] (Mock Mode) Dispatched to {}: wamid={}", recipientPhone, mockWamid);
            return AlertDispatchResult.success(mockWamid, "{\"messages\":[{\"id\":\"" + mockWamid + "\"}]}");
        }

        try {
            String jsonPayload;
            if (request.template() != null && request.template().getChannel() == AlertChannel.WHATSAPP) {
                List<AlertButtonEntity> buttons = request.template().getButtons();
                if (buttons != null && buttons.size() > 3) {
                    jsonPayload = buildInteractiveListPayload(recipientPhone, renderedText, buttons);
                } else {
                    String templateName = request.template().getTemplateKey().toLowerCase();
                    String langCode = request.template().getLanguage() != null ? request.template().getLanguage() : "en_US";
                    jsonPayload = String.format(
                            "{\"messaging_product\":\"whatsapp\",\"recipient_type\":\"individual\",\"to\":\"%s\",\"type\":\"template\",\"template\":{\"name\":\"%s\",\"language\":{\"code\":\"%s\"}}}",
                            recipientPhone.replace("+", ""),
                            templateName,
                            langCode
                    );
                }
            } else {
                jsonPayload = String.format(
                        "{\"messaging_product\":\"whatsapp\",\"recipient_type\":\"individual\",\"to\":\"%s\",\"type\":\"text\",\"text\":{\"preview_url\":false,\"body\":\"%s\"}}",
                        recipientPhone.replace("+", ""),
                        escapeJson(renderedText)
                );
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

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractWamid(String json) {
        if (json != null && json.contains("\"id\":")) {
            int start = json.indexOf("\"id\":\"") + 6;
            int end = json.indexOf("\"", start);
            if (start > 5 && end > start) {
                return json.substring(start, end);
            }
        }
        return "wamid.HBgL" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public String buildInteractiveListPayload(String recipientPhone, String bodyText, List<AlertButtonEntity> buttons) {
        StringBuilder rowsJson = new StringBuilder();
        if (buttons != null) {
            for (int i = 0; i < buttons.size(); i++) {
                AlertButtonEntity b = buttons.get(i);
                if (i > 0) rowsJson.append(",");
                rowsJson.append(String.format(
                        "{\"id\":\"%s\",\"title\":\"%s\"}",
                        escapeJson(b.getPayload() != null ? b.getPayload() : "opt_" + b.getSequenceOrder()),
                        escapeJson(b.getButtonText() != null ? b.getButtonText() : "Option " + (i + 1))
                ));
            }
        }

        return String.format(
                "{\"messaging_product\":\"whatsapp\",\"recipient_type\":\"individual\",\"to\":\"%s\",\"type\":\"interactive\",\"interactive\":{\"type\":\"list\",\"body\":{\"text\":\"%s\"},\"action\":{\"button\":\"Select Option\",\"sections\":[{\"title\":\"Options\",\"rows\":[%s]}]}}}",
                recipientPhone.replace("+", ""),
                escapeJson(bodyText != null && !bodyText.isBlank() ? bodyText : "Please select an option:"),
                rowsJson.toString()
        );
    }
}
