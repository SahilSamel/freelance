package com.freelance.alerts.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.ConfigurationEngine;
import com.freelance.keys.alerts.webhook.WebhookConfigKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class WhatsAppMediaService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppMediaService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient;

    public record DownloadedMedia(byte[] data, String mimeType, String fileName) {}
    public record UploadedMedia(String mediaId) {}

    public WhatsAppMediaService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public WhatsAppMediaService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public DownloadedMedia downloadMedia(String mediaId) throws IOException, InterruptedException {
        String accessToken = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN);
        if (accessToken == null || accessToken.isBlank() || accessToken.equals(WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getDefaultValue())) {
            throw new IOException("WhatsApp access token is not configured");
        }

        String baseUrl = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_API_BASE_URL);
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://graph.facebook.com/v20.0";
        }

        // Step 1: Query Graph API for temporary download URL
        URI metaUrlUri = URI.create(baseUrl + "/" + mediaId);
        HttpRequest metaRequest = HttpRequest.newBuilder()
                .uri(metaUrlUri)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> metaResponse = httpClient.send(metaRequest, HttpResponse.BodyHandlers.ofString());
        if (metaResponse.statusCode() < 200 || metaResponse.statusCode() >= 300) {
            throw new IOException("Failed to retrieve media metadata from Meta (HTTP " + metaResponse.statusCode() + "): " + metaResponse.body());
        }

        JsonNode root = objectMapper.readTree(metaResponse.body());
        String downloadUrl = root.path("url").asText(null);
        String mimeType = root.path("mime_type").asText("application/octet-stream");
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IOException("Meta media response did not contain download URL: " + metaResponse.body());
        }

        // Step 2: Fetch the actual media binary stream
        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<byte[]> downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (downloadResponse.statusCode() < 200 || downloadResponse.statusCode() >= 300) {
            throw new IOException("Failed to download media bytes from Meta (HTTP " + downloadResponse.statusCode() + ")");
        }

        String extension = MediaStorageService.getExtensionForMimeType(mimeType);
        String fileName = mediaId + extension;
        return new DownloadedMedia(downloadResponse.body(), mimeType, fileName);
    }

    public UploadedMedia uploadMedia(String fileName, String mimeType, byte[] data) throws IOException, InterruptedException {
        String accessToken = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN);
        if (accessToken == null || accessToken.isBlank() || accessToken.equals(WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getDefaultValue())) {
            throw new IOException("WhatsApp access token is not configured");
        }

        String phoneNumberId = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_PHONE_NUMBER_ID);
        if (phoneNumberId == null || phoneNumberId.isBlank() || phoneNumberId.equals(WebhookConfigKeys.WHATSAPP_PHONE_NUMBER_ID.getDefaultValue())) {
            throw new IOException("WhatsApp phone number ID is not configured");
        }

        String baseUrl = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_API_BASE_URL);
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://graph.facebook.com/v20.0";
        }

        String boundary = "----FreelanceMediaBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] multipartBody = buildMultipartBody(boundary, fileName, mimeType, data);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + phoneNumberId + "/media"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Failed to upload media to Meta (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String mediaId = root.path("id").asText(null);
        if (mediaId == null || mediaId.isBlank()) {
            throw new IOException("Meta upload response missing media 'id': " + response.body());
        }

        return new UploadedMedia(mediaId);
    }

    private byte[] buildMultipartBody(String boundary, String fileName, String mimeType, byte[] fileData) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String typeValue = (mimeType != null && mimeType.toLowerCase().startsWith("image/")) ? "image" : "document";

        // field: messaging_product
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write("Content-Disposition: form-data; name=\"messaging_product\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.write("whatsapp\r\n".getBytes(StandardCharsets.UTF_8));

        // field: type
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write("Content-Disposition: form-data; name=\"type\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.write((typeValue + "\r\n").getBytes(StandardCharsets.UTF_8));

        // field: file
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(fileData);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));

        // boundary end
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
