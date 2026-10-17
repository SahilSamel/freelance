package com.freelance.alerts.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freelance.alerts.dto.webhook.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight, in-process Mock HTTP Server simulating the MSG91 / Meta WhatsApp Cloud API gateway.
 * <p>
 * Features:
 * <ul>
 *   <li>Listens on an ephemeral or configured port and receives real HTTP POST requests from {@code Msg91WhatsAppDispatcherService}</li>
 *   <li>Validates required headers ({@code authkey}, {@code Content-Type}) and parses template parameters</li>
 *   <li>Stores all incoming requests in-memory for assertion and inspection</li>
 *   <li>Responds with authentic MSG91/Meta JSON containing a unique {@code request_id} (wamid)</li>
 *   <li>Provides helper factory methods to generate simulated delivery status updates and customer inbound messages</li>
 *   <li>Can also send outbound HTTP POST callbacks directly to a running webhook endpoint</li>
 * </ul>
 * </p>
 */
public class MockWhatsAppHttpServer implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MockWhatsAppHttpServer.class);

    public record RecordedOutboundMessage(
            String id,
            String to,
            String templateName,
            String language,
            String contentType,
            String authKey,
            String mediaType,
            String mediaUrl,
            Map<String, String> bodyVariables,
            List<String> buttonPayloads,
            String rawPayload,
            Instant receivedAt
    ) {}

    private final int requestedPort;
    private final ObjectMapper objectMapper;
    private final List<RecordedOutboundMessage> recordedMessages;
    private final AtomicInteger messageCounter;
    private final HttpClient httpClient;

    private HttpServer server;
    private int boundPort;

    public MockWhatsAppHttpServer() {
        this(0);
    }

    public MockWhatsAppHttpServer(int port) {
        this.requestedPort = port;
        this.objectMapper = new ObjectMapper();
        this.recordedMessages = new CopyOnWriteArrayList<>();
        this.messageCounter = new AtomicInteger(1000);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress(requestedPort), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/", new OutboundMessageHandler());

        server.start();
        boundPort = server.getAddress().getPort();
        logger.info("[MockWhatsAppHttpServer] Started on port {}, Base URL: {}", boundPort, getBaseUrl());
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            logger.info("[MockWhatsAppHttpServer] Stopped.");
        }
    }

    @Override
    public void close() {
        stop();
    }

    public int getPort() {
        return boundPort > 0 ? boundPort : requestedPort;
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + getPort() + "/api/v5/whatsapp";
    }

    public List<RecordedOutboundMessage> getRecordedMessages() {
        return Collections.unmodifiableList(recordedMessages);
    }

    public Optional<RecordedOutboundMessage> getLastMessage() {
        if (recordedMessages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(recordedMessages.get(recordedMessages.size() - 1));
    }

    public List<RecordedOutboundMessage> findMessagesForTemplate(String templateName) {
        if (templateName == null) return List.of();
        return recordedMessages.stream()
                .filter(m -> templateName.equalsIgnoreCase(m.templateName()))
                .toList();
    }

    public Optional<RecordedOutboundMessage> findMessageById(String messageId) {
        if (messageId == null) return Optional.empty();
        return recordedMessages.stream()
                .filter(m -> messageId.equalsIgnoreCase(m.id()))
                .findFirst();
    }

    public void clear() {
        recordedMessages.clear();
    }

    // ── HTTP Request Handler ──────────────────────────────────────────────────

    private class OutboundMessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("POST".equalsIgnoreCase(method) && (path.contains("whatsapp-outbound-message") || path.equals("/") || path.endsWith("/"))) {
                handleOutboundDispatch(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.endsWith("/messages")) {
                handleGetMessages(exchange);
            } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/clear")) {
                clear();
                sendJsonResponse(exchange, 200, "{\"status\":\"cleared\"}");
            } else {
                sendJsonResponse(exchange, 404, "{\"error\":\"Not Found\",\"path\":\"" + path + "\"}");
            }
        }

        private void handleOutboundDispatch(HttpExchange exchange) throws IOException {
            String authKey = exchange.getRequestHeaders().getFirst("authkey");
            if (authKey == null || authKey.isBlank()) {
                authKey = exchange.getRequestHeaders().getFirst("Authorization");
            }

            InputStream is = exchange.getRequestBody();
            String rawJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            String wamid = "wamid.mock." + UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                    + "_" + messageCounter.incrementAndGet();

            String to = null;
            String templateName = null;
            String language = null;
            String contentType = null;
            String mediaType = null;
            String mediaUrl = null;
            Map<String, String> bodyVariables = new LinkedHashMap<>();
            List<String> buttonPayloads = new ArrayList<>();

            try {
                JsonNode root = objectMapper.readTree(rawJson);
                contentType = root.path("content_type").asText(null);

                JsonNode payloadNode = root.path("payload");
                to = payloadNode.path("to").asText(null);

                JsonNode tplNode = payloadNode.path("template");
                if (!tplNode.isMissingNode()) {
                    templateName = tplNode.path("name").asText(null);
                    language = tplNode.path("language").path("code").asText(null);

                    JsonNode toAndComponents = tplNode.path("to_and_components");
                    if (toAndComponents.isArray() && !toAndComponents.isEmpty()) {
                        JsonNode firstRecipient = toAndComponents.get(0);
                        JsonNode components = firstRecipient.path("components");

                        if (components.isObject()) {
                            Iterator<Map.Entry<String, JsonNode>> fields = components.fields();
                            while (fields.hasNext()) {
                                Map.Entry<String, JsonNode> entry = fields.next();
                                String compKey = entry.getKey();
                                JsonNode compVal = entry.getValue();

                                if (compKey.startsWith("header_")) {
                                    mediaType = compVal.path("type").asText(null);
                                    mediaUrl = compVal.path("value").asText(null);
                                } else if (compKey.startsWith("body_")) {
                                    String text = compVal.path("value").asText("");
                                    bodyVariables.put(compKey, text);
                                } else if (compKey.startsWith("button_")) {
                                    String btnVal = compVal.path("value").asText("");
                                    buttonPayloads.add(btnVal);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("[MockWhatsAppHttpServer] Failed to fully parse outbound JSON payload: {}", rawJson, e);
            }

            RecordedOutboundMessage recorded = new RecordedOutboundMessage(
                    wamid,
                    to,
                    templateName,
                    language,
                    contentType,
                    authKey,
                    mediaType,
                    mediaUrl,
                    bodyVariables,
                    buttonPayloads,
                    rawJson,
                    Instant.now()
            );
            recordedMessages.add(recorded);

            logger.info("[MockWhatsAppHttpServer] Dispatched template '{}' to recipient '{}' -> assigned wamid: {}",
                    templateName, to, wamid);

            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("status", "success");
            responseNode.put("message", "Message queued successfully");
            responseNode.put("request_id", wamid);

            sendJsonResponse(exchange, 200, responseNode.toString());
        }

        private void handleGetMessages(HttpExchange exchange) throws IOException {
            String json = objectMapper.writeValueAsString(recordedMessages);
            sendJsonResponse(exchange, 200, json);
        }

        private void sendJsonResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ── Webhook Simulation Factories ──────────────────────────────────────────

    /**
     * Builds a Meta/MSG91 delivery status update webhook payload (e.g. status: "delivered", "read", "sent").
     */
    public WhatsAppWebhookPayloadDTO createStatusWebhookPayload(String messageId, String recipientPhone, String status) {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        WebhookStatusDTO statusDTO = new WebhookStatusDTO(messageId, status, ts, recipientPhone, null);
        WebhookValueDTO value = new WebhookValueDTO(
                "whatsapp",
                new WebhookMetadataDTO("919876543210", "100200300"),
                List.of(statusDTO),
                null,
                null
        );
        WebhookChangeDTO change = new WebhookChangeDTO("messages", value);
        WebhookEntryDTO entry = new WebhookEntryDTO("entry_" + UUID.randomUUID().toString().substring(0, 8), List.of(change));
        return new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entry));
    }

    /**
     * Builds an inbound customer freeform text message webhook payload.
     */
    public WhatsAppWebhookPayloadDTO createCustomerTextPayload(String fromPhone, String text) {
        String msgId = "wamid.inbound." + UUID.randomUUID().toString().substring(0, 12);
        String ts = String.valueOf(Instant.now().getEpochSecond());
        WebhookTextMessageDTO textDTO = new WebhookTextMessageDTO(text);
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                fromPhone,
                msgId,
                ts,
                "text",
                textDTO,
                null,
                null
        );

        WebhookValueDTO value = new WebhookValueDTO(
                "whatsapp",
                new WebhookMetadataDTO("919876543210", "100200300"),
                null,
                List.of(msgDTO),
                null
        );
        WebhookChangeDTO change = new WebhookChangeDTO("messages", value);
        WebhookEntryDTO entry = new WebhookEntryDTO("entry_" + UUID.randomUUID().toString().substring(0, 8), List.of(change));
        return new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entry));
    }

    /**
     * Builds an inbound interactive button click webhook payload.
     */
    public WhatsAppWebhookPayloadDTO createCustomerButtonPayload(String fromPhone, String buttonPayload, String buttonTitle) {
        String msgId = "wamid.inbound.btn." + UUID.randomUUID().toString().substring(0, 12);
        String ts = String.valueOf(Instant.now().getEpochSecond());
        WebhookButtonDTO buttonDTO = new WebhookButtonDTO(buttonTitle, buttonPayload);
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                fromPhone,
                msgId,
                ts,
                "button",
                null,
                null,
                buttonDTO
        );

        WebhookValueDTO value = new WebhookValueDTO(
                "whatsapp",
                new WebhookMetadataDTO("919876543210", "100200300"),
                null,
                List.of(msgDTO),
                null
        );
        WebhookChangeDTO change = new WebhookChangeDTO("messages", value);
        WebhookEntryDTO entry = new WebhookEntryDTO("entry_" + UUID.randomUUID().toString().substring(0, 8), List.of(change));
        return new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entry));
    }

    /**
     * Builds an inbound image media webhook payload (e.g. payment receipt screenshot).
     */
    public WhatsAppWebhookPayloadDTO createCustomerImagePayload(String fromPhone, String mediaId, String caption) {
        String msgId = "wamid.inbound.img." + UUID.randomUUID().toString().substring(0, 12);
        String ts = String.valueOf(Instant.now().getEpochSecond());
        WebhookMediaDTO mediaDTO = new WebhookMediaDTO(mediaId, "image/jpeg", "sha256_mock", caption, "image.jpg");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                fromPhone,
                msgId,
                ts,
                "image",
                null,
                null,
                null,
                mediaDTO,
                null
        );

        WebhookValueDTO value = new WebhookValueDTO(
                "whatsapp",
                new WebhookMetadataDTO("919876543210", "100200300"),
                null,
                List.of(msgDTO),
                null
        );
        WebhookChangeDTO change = new WebhookChangeDTO("messages", value);
        WebhookEntryDTO entry = new WebhookEntryDTO("entry_" + UUID.randomUUID().toString().substring(0, 8), List.of(change));
        return new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entry));
    }

    /**
     * Sends an HTTP POST callback directly to an external or running server's webhook endpoint.
     */
    public boolean postWebhookCallback(String webhookEndpointUrl, WhatsAppWebhookPayloadDTO payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookEndpointUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            logger.error("[MockWhatsAppHttpServer] Failed to deliver webhook callback to {}", webhookEndpointUrl, e);
            return false;
        }
    }

    // ── Standalone CLI Runner ─────────────────────────────────────────────────

    public static void main(String[] args) {
        int port = 8089;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        try {
            MockWhatsAppHttpServer server = new MockWhatsAppHttpServer(port);
            server.start();
            System.out.println("================================================================================");
            System.out.println("  Mock WhatsApp HTTP Server started on: " + server.getBaseUrl());
            System.out.println("  Listening for outbound messages at: " + server.getBaseUrl() + "/whatsapp-outbound-message/");
            System.out.println("  Press Ctrl+C to terminate the mock server.");
            System.out.println("================================================================================");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Mock WhatsApp HTTP Server...");
                server.stop();
            }));

            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Failed to start Mock WhatsApp HTTP Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
