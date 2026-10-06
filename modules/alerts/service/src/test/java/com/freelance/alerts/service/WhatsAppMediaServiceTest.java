package com.freelance.alerts.service;

import com.freelance.alerts.media.WhatsAppMediaService;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.webhook.WebhookConfigKeys;
import com.freelance.manager.CacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import static org.junit.jupiter.api.Assertions.*;

public class WhatsAppMediaServiceTest {

    private static final String SYSTEM_CONFIG_CACHE = "cache_SystemConfig";

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        try {
            com.freelance.config.CacheRegistry.getInstance().init("com.freelance.fixtures");
        } catch (Exception ignored) {
        }
        CacheManager.put(SYSTEM_CONFIG_CACHE, WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getKey(), "test_valid_meta_token_123");
        CacheManager.put(SYSTEM_CONFIG_CACHE, WebhookConfigKeys.WHATSAPP_PHONE_NUMBER_ID.getKey(), "10987654321");
    }

    @AfterEach
    void tearDown() {
        try {
            CacheManager.invalidate(SYSTEM_CONFIG_CACHE, WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getKey());
            CacheManager.invalidate(SYSTEM_CONFIG_CACHE, WebhookConfigKeys.WHATSAPP_PHONE_NUMBER_ID.getKey());
        } catch (Exception ignored) {
        }
        TestContextHelper.tearDown();
    }

    static class MockHttpResponse<T> implements HttpResponse<T> {
        private final int statusCode;
        private final T body;

        MockHttpResponse(int statusCode, T body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override public int statusCode() { return statusCode; }
        @Override public HttpRequest request() { return null; }
        @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Collections.emptyMap(), (k, v) -> true); }
        @Override public T body() { return body; }
        @Override public Optional<javax.net.ssl.SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return URI.create("https://graph.facebook.com/v20.0"); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }

    static class MultiStepFakeHttpClient extends HttpClient {
        int requestCount = 0;
        HttpRequest lastRequest;

        int firstStatus = 200;
        Object firstBody = "{}";

        int secondStatus = 200;
        Object secondBody = new byte[0];

        MultiStepFakeHttpClient(int firstStatus, Object firstBody) {
            this.firstStatus = firstStatus;
            this.firstBody = firstBody;
        }

        MultiStepFakeHttpClient(int firstStatus, Object firstBody, int secondStatus, Object secondBody) {
            this.firstStatus = firstStatus;
            this.firstBody = firstBody;
            this.secondStatus = secondStatus;
            this.secondBody = secondBody;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseHandler) {
            this.lastRequest = request;
            requestCount++;
            if (requestCount == 1) {
                return new MockHttpResponse<>(firstStatus, (T) firstBody);
            }
            return new MockHttpResponse<>(secondStatus, (T) secondBody);
        }

        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return null; }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseHandler) { return null; }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) { return null; }
    }

    @Test
    @DisplayName("Should throw IOException when access token is missing or placeholder")
    void testMissingAccessTokenThrowsIOException() {
        CacheManager.put(SYSTEM_CONFIG_CACHE, WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getKey(), WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getDefaultValue());

        WhatsAppMediaService service = new WhatsAppMediaService();
        IOException exUpload = assertThrows(IOException.class, () -> service.uploadMedia("file.jpg", "image/jpeg", new byte[]{1}));
        assertTrue(exUpload.getMessage().contains("WhatsApp access token is not configured"));

        IOException exDownload = assertThrows(IOException.class, () -> service.downloadMedia("12345"));
        assertTrue(exDownload.getMessage().contains("WhatsApp access token is not configured"));
    }

    @Test
    @DisplayName("Should throw IOException when phone number ID is missing on upload")
    void testMissingPhoneNumberIdThrowsIOException() {
        CacheManager.put(SYSTEM_CONFIG_CACHE, WebhookConfigKeys.WHATSAPP_PHONE_NUMBER_ID.getKey(), WebhookConfigKeys.WHATSAPP_PHONE_NUMBER_ID.getDefaultValue());

        WhatsAppMediaService service = new WhatsAppMediaService();
        IOException ex = assertThrows(IOException.class, () -> service.uploadMedia("file.jpg", "image/jpeg", new byte[]{1}));
        assertTrue(ex.getMessage().contains("WhatsApp phone number ID is not configured"));
    }

    @Test
    @DisplayName("Should upload media to Meta Graph API and return assigned media ID")
    void testUploadMediaSuccess() throws IOException, InterruptedException {
        MultiStepFakeHttpClient fakeClient = new MultiStepFakeHttpClient(200, "{\"id\":\"meta_media_live_999\"}");
        WhatsAppMediaService service = new WhatsAppMediaService(fakeClient);

        byte[] payload = "actual image binary".getBytes();
        WhatsAppMediaService.UploadedMedia result = service.uploadMedia("photo.jpg", "image/jpeg", payload);

        assertNotNull(result);
        assertEquals("meta_media_live_999", result.mediaId());
        assertNotNull(fakeClient.lastRequest);
        assertTrue(fakeClient.lastRequest.uri().toString().contains("/10987654321/media"));
        assertTrue(fakeClient.lastRequest.headers().firstValue("Authorization").orElse("").contains("test_valid_meta_token_123"));
    }

    @Test
    @DisplayName("Should throw IOException when Meta returns non-200 HTTP status on upload")
    void testUploadMediaFailureThrowsIOException() {
        MultiStepFakeHttpClient fakeClient = new MultiStepFakeHttpClient(401, "{\"error\":{\"message\":\"Invalid OAuth access token\"}}");
        WhatsAppMediaService service = new WhatsAppMediaService(fakeClient);

        IOException ex = assertThrows(IOException.class, () ->
                service.uploadMedia("photo.jpg", "image/jpeg", new byte[]{1, 2, 3})
        );
        assertTrue(ex.getMessage().contains("Failed to upload media to Meta (HTTP 401)"));
    }

    @Test
    @DisplayName("Should download media via 2-step Meta Graph API flow and return bytes")
    void testDownloadMediaSuccess() throws IOException, InterruptedException {
        String metaMetadataJson = "{\"url\":\"https://lookaside.fbsbx.com/sample_download_url\",\"mime_type\":\"image/jpeg\"}";
        byte[] expectedBytes = new byte[]{10, 20, 30, 40};

        MultiStepFakeHttpClient fakeClient = new MultiStepFakeHttpClient(200, metaMetadataJson, 200, expectedBytes);
        WhatsAppMediaService service = new WhatsAppMediaService(fakeClient);

        WhatsAppMediaService.DownloadedMedia result = service.downloadMedia("media_id_777");

        assertNotNull(result);
        assertArrayEquals(expectedBytes, result.data());
        assertEquals("image/jpeg", result.mimeType());
        assertEquals("media_id_777.jpg", result.fileName());
        assertEquals(2, fakeClient.requestCount);
    }

    @Test
    @DisplayName("Should throw IOException when Meta metadata query fails on download")
    void testDownloadMediaMetadataFailureThrowsIOException() {
        MultiStepFakeHttpClient fakeClient = new MultiStepFakeHttpClient(404, "{\"error\":{\"message\":\"Media not found\"}}");
        WhatsAppMediaService service = new WhatsAppMediaService(fakeClient);

        IOException ex = assertThrows(IOException.class, () -> service.downloadMedia("unknown_id"));
        assertTrue(ex.getMessage().contains("Failed to retrieve media metadata from Meta (HTTP 404)"));
    }
}
