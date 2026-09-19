package com.freelance.alerts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppTemplateService;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppTemplateService.TemplateSubmissionResult;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppTemplateServiceTest {

    private TransactionContext context;
    private final ObjectMapper objectMapper = new ObjectMapper();

    static class FakeHttpResponse<T> implements HttpResponse<T> {
        private final int statusCode;
        private final T body;

        FakeHttpResponse(int statusCode, T body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override public int statusCode() { return statusCode; }
        @Override public T body() { return body; }
        @Override public HttpRequest request() { return null; }
        @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (k, v) -> true); }
        @Override public URI uri() { return URI.create("https://graph.facebook.com"); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }

    static class FakeHttpClient extends HttpClient {
        int statusCode = 200;
        String responseBody = "{}";
        IOException toThrow = null;
        HttpRequest lastRequest;

        FakeHttpClient(int statusCode, String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        FakeHttpClient(IOException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseHandler) throws IOException {
            this.lastRequest = request;
            if (toThrow != null) {
                throw toThrow;
            }
            return new FakeHttpResponse<>(statusCode, (T) responseBody);
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

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should return mock success in mock mode with placeholder token")
    void testMockSubmission() {
        WhatsAppTemplateService service = new WhatsAppTemplateService();

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_MOCK_TEST",
                "Mock Test",
                "UTILITY",
                "en_US",
                "Hello {{user.name}}",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                "user.name",
                null
        );

        TemplateSubmissionResult result = service.submitTemplate(entity, null);

        assertTrue(result.successful());
        assertNotNull(result.metaTemplateId());
        assertTrue(result.metaTemplateId().startsWith("meta_tpl_"));
        assertEquals(TemplateStatus.SUBMITTED, result.status());
    }

    @Test
    @DisplayName("Should build valid Meta payload with named body variables converted to indexed and examples")
    void testBuildPayloadWithBodyVariables() throws Exception {
        WhatsAppTemplateService service = new WhatsAppTemplateService();

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_ORDER_CONFIRM",
                "Order Confirm",
                "UTILITY",
                "en_US",
                "Hello {{user.name}}, your order {{order.id}} has been confirmed!",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                "user.name,order.id",
                null
        );

        String jsonPayload = service.buildPayload(entity, null);
        assertNotNull(jsonPayload);

        JsonNode root = objectMapper.readTree(jsonPayload);
        assertEquals("tpl_order_confirm", root.get("name").asText());
        assertEquals("UTILITY", root.get("category").asText());
        assertEquals("en_US", root.get("language").asText());

        JsonNode components = root.get("components");
        assertEquals(1, components.size());

        JsonNode body = components.get(0);
        assertEquals("BODY", body.get("type").asText());
        assertEquals("Hello {{1}}, your order {{2}} has been confirmed!", body.get("text").asText());

        JsonNode example = body.get("example");
        assertNotNull(example);
        JsonNode bodyText = example.get("body_text");
        assertNotNull(bodyText);
        JsonNode sampleRow = bodyText.get(0);
        assertEquals(2, sampleRow.size());
        assertEquals("user.name", sampleRow.get(0).asText());
        assertEquals("order.id", sampleRow.get(1).asText());
    }

    @Test
    @DisplayName("Should deduplicate repeated variable tokens in body and provide single example")
    void testBuildPayloadWithDeduplicatedVariables() throws Exception {
        WhatsAppTemplateService service = new WhatsAppTemplateService();

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_REPEAT",
                "Repeat",
                "MARKETING",
                "en",
                "Dear {{customer}}, thank you {{customer}} for your loyalty!",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );

        String jsonPayload = service.buildPayload(entity, null);
        JsonNode root = objectMapper.readTree(jsonPayload);
        assertEquals("MARKETING", root.get("category").asText());

        JsonNode body = root.get("components").get(0);
        assertEquals("Dear {{1}}, thank you {{1}} for your loyalty!", body.get("text").asText());

        JsonNode sampleRow = body.get("example").get("body_text").get(0);
        assertEquals(1, sampleRow.size());
        assertEquals("customer", sampleRow.get(0).asText());
    }

    @Test
    @DisplayName("Should not include example block if body has no variables")
    void testBuildPayloadNoVariables() throws Exception {
        WhatsAppTemplateService service = new WhatsAppTemplateService();

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_STATIC",
                "Static",
                "UTILITY",
                "en_US",
                "Welcome to our service!",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );

        String jsonPayload = service.buildPayload(entity, null);
        JsonNode root = objectMapper.readTree(jsonPayload);

        JsonNode body = root.get("components").get(0);
        assertEquals("BODY", body.get("type").asText());
        assertEquals("Welcome to our service!", body.get("text").asText());
        assertFalse(body.has("example"));
    }

    @Test
    @DisplayName("Should build payload with HEADER and multiple BUTTON types")
    void testBuildPayloadWithHeaderAndButtons() throws Exception {
        WhatsAppTemplateService service = new WhatsAppTemplateService();

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_INTERACTIVE",
                "Interactive",
                "UTILITY",
                "en_US",
                "Click a button below",
                "https://example.com/image.png",
                "IMAGE",
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );

        List<TemplateButtonDTO> buttons = List.of(
                new TemplateButtonDTO("Confirm", ButtonType.QUICK_REPLY, "p_confirm", null, false, 1),
                new TemplateButtonDTO("Website", ButtonType.URL, "https://example.com", null, false, 2),
                new TemplateButtonDTO("Support", ButtonType.PHONE_NUMBER, "+18001234567", null, false, 3)
        );

        String jsonPayload = service.buildPayload(entity, buttons);
        JsonNode root = objectMapper.readTree(jsonPayload);

        JsonNode components = root.get("components");
        assertEquals(3, components.size());

        JsonNode header = components.get(0);
        assertEquals("HEADER", header.get("type").asText());
        assertEquals("IMAGE", header.get("format").asText());

        JsonNode body = components.get(1);
        assertEquals("BODY", body.get("type").asText());

        JsonNode buttonsNode = components.get(2);
        assertEquals("BUTTONS", buttonsNode.get("type").asText());
        JsonNode btnList = buttonsNode.get("buttons");
        assertEquals(3, btnList.size());

        assertEquals("QUICK_REPLY", btnList.get(0).get("type").asText());
        assertEquals("Confirm", btnList.get(0).get("text").asText());

        assertEquals("URL", btnList.get(1).get("type").asText());
        assertEquals("https://example.com", btnList.get(1).get("url").asText());

        assertEquals("PHONE_NUMBER", btnList.get(2).get("type").asText());
        assertEquals("+18001234567", btnList.get(2).get("phone_number").asText());
    }

    @Test
    @DisplayName("Should parse HTTP 200 with PENDING status as SUBMITTED in live mode")
    void testLiveSubmissionPending() {
        FakeHttpClient fakeClient = new FakeHttpClient(200, "{\"id\":\"9876543210\",\"status\":\"PENDING\",\"category\":\"UTILITY\"}");
        WhatsAppTemplateService service = new WhatsAppTemplateService(fakeClient, true);

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_LIVE_PENDING",
                "Live Pending",
                "UTILITY",
                "en_US",
                "Test content",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );

        TemplateSubmissionResult result = service.submitTemplate(entity, null);

        assertTrue(result.successful());
        assertEquals("9876543210", result.metaTemplateId());
        assertEquals(TemplateStatus.SUBMITTED, result.status());
        assertNotNull(fakeClient.lastRequest);
        assertTrue(fakeClient.lastRequest.uri().toString().contains("/message_templates"));
    }

    @Test
    @DisplayName("Should parse HTTP 200 with APPROVED status as APPROVED in live mode")
    void testLiveSubmissionApproved() {
        FakeHttpClient fakeClient = new FakeHttpClient(200, "{\"id\":\"5555555555\",\"status\":\"APPROVED\",\"category\":\"UTILITY\"}");
        WhatsAppTemplateService service = new WhatsAppTemplateService(fakeClient, true);

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_LIVE_APPROVED",
                "Live Approved",
                "UTILITY",
                "en_US",
                "Instant approval content",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );

        TemplateSubmissionResult result = service.submitTemplate(entity, null);

        assertTrue(result.successful());
        assertEquals("5555555555", result.metaTemplateId());
        assertEquals(TemplateStatus.APPROVED, result.status());
    }

    @Test
    @DisplayName("Should return failure with error message on HTTP 400 from Meta")
    void testLiveSubmissionHttp400() {
        FakeHttpClient fakeClient = new FakeHttpClient(400, "{\"error\":{\"message\":\"Template name already exists in account\",\"type\":\"OAuthException\",\"code\":100}}");
        WhatsAppTemplateService service = new WhatsAppTemplateService(fakeClient, true);

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_LIVE_FAIL",
                "Live Fail",
                "UTILITY",
                "en_US",
                "Content",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );

        TemplateSubmissionResult result = service.submitTemplate(entity, null);

        assertFalse(result.successful());
        assertNull(result.metaTemplateId());
        assertTrue(result.errorMessage().contains("Template name already exists in account"));
    }

    @Test
    @DisplayName("Should return failure when HttpClient throws network exception")
    void testLiveSubmissionNetworkException() {
        FakeHttpClient fakeClient = new FakeHttpClient(new IOException("Connection reset by peer"));
        WhatsAppTemplateService service = new WhatsAppTemplateService(fakeClient, true);

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_NET_ERR",
                "Net Err",
                "UTILITY",
                "en_US",
                "Content",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );

        TemplateSubmissionResult result = service.submitTemplate(entity, null);

        assertFalse(result.successful());
        assertTrue(result.errorMessage().contains("Connection reset by peer"));
    }

    @Test
    @DisplayName("Should auto-approve template locally when buttons size > 3 (interactive list)")
    void testAutoApproveInteractiveListTemplate() {
        WhatsAppTemplateService service = new WhatsAppTemplateService();

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_LIST_MENU",
                "List Menu",
                "UTILITY",
                "en_US",
                "Choose from our services:",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );

        List<TemplateButtonDTO> fiveButtons = List.of(
                new TemplateButtonDTO("Option 1", ButtonType.QUICK_REPLY, "p1", null, false, 1),
                new TemplateButtonDTO("Option 2", ButtonType.QUICK_REPLY, "p2", null, false, 2),
                new TemplateButtonDTO("Option 3", ButtonType.QUICK_REPLY, "p3", null, false, 3),
                new TemplateButtonDTO("Option 4", ButtonType.QUICK_REPLY, "p4", null, false, 4),
                new TemplateButtonDTO("Option 5", ButtonType.QUICK_REPLY, "p5", null, false, 5)
        );

        TemplateSubmissionResult result = service.submitTemplate(entity, fiveButtons);

        assertTrue(result.successful());
        assertNotNull(result.metaTemplateId());
        assertTrue(result.metaTemplateId().startsWith("local_list_"));
        assertEquals(TemplateStatus.APPROVED, result.status());
    }

    @Test
    @DisplayName("Should test all constructors of WhatsAppTemplateService")
    void testConstructors() {
        FakeHttpClient client = new FakeHttpClient(200, "{}");
        assertNotNull(new WhatsAppTemplateService(client));
        assertNotNull(new WhatsAppTemplateService(client, TemplateStatus.APPROVED));
        assertNotNull(new WhatsAppTemplateService(client, new ObjectMapper(), TemplateStatus.DRAFT));
    }

    @Test
    @DisplayName("submitTemplate returns failure when entity is null")
    void testSubmitTemplateNullEntity() {
        WhatsAppTemplateService service = new WhatsAppTemplateService();
        TemplateSubmissionResult result = service.submitTemplate(null, null);
        assertFalse(result.successful());
        assertEquals("Template entity cannot be null", result.errorMessage());
    }

    @Test
    @DisplayName("formatMetaTemplateName handles null and empty keys")
    void testFormatMetaTemplateName() {
        WhatsAppTemplateService service = new WhatsAppTemplateService();
        assertTrue(service.formatMetaTemplateName(null).startsWith("template_"));
        assertTrue(service.formatMetaTemplateName("").startsWith("template_"));
        assertEquals("my_template_name", service.formatMetaTemplateName("MY-TEMPLATE.NAME"));
    }

    @Test
    @DisplayName("formatBodyTextAndExamples handles null and blank inputs")
    void testFormatBodyTextAndExamplesNullOrBlank() {
        WhatsAppTemplateService service = new WhatsAppTemplateService();
        WhatsAppTemplateService.BodyFormatResult res1 = service.formatBodyTextAndExamples(null);
        assertEquals("", res1.formattedText());
        assertTrue(res1.exampleValues().isEmpty());

        WhatsAppTemplateService.BodyFormatResult res2 = service.formatBodyTextAndExamples("   ");
        assertEquals("", res2.formattedText());
        assertTrue(res2.exampleValues().isEmpty());

        // Body with digits placeholder
        WhatsAppTemplateService.BodyFormatResult res3 = service.formatBodyTextAndExamples("Code: {{123}}");
        assertEquals("Code: {{1}}", res3.formattedText());
        assertEquals("sample_123", res3.exampleValues().get(0));
    }

    @Test
    @DisplayName("buildPayload handles AUTH category and 4-arg constructor")
    void testAuthCategoryAndFourArgConstructor() {
        FakeHttpClient client = new FakeHttpClient(200, "{}");
        WhatsAppTemplateService service = new WhatsAppTemplateService(client, new ObjectMapper(), TemplateStatus.APPROVED, false);
        assertNotNull(service);

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_AUTH", "Auth Tpl", "AUTHENTICATION", "en_US", "OTP: {{code}}",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, null
        );
        String json = service.buildPayload(entity, null);
        assertTrue(json.contains("\"AUTHENTICATION\""));
    }

    @Test
    @DisplayName("submitTemplate handles non-JSON error response from Meta API")
    void testNonJsonErrorResponse() {
        FakeHttpClient fakeClient = new FakeHttpClient(502, "Bad Gateway Plain Text");
        WhatsAppTemplateService service = new WhatsAppTemplateService(fakeClient, true);

        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_GATEWAY_ERR", "Title", "UTILITY", "en_US", "Body",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, null
        );

        TemplateSubmissionResult result = service.submitTemplate(entity, null);
        assertFalse(result.successful());
        assertEquals("Bad Gateway Plain Text", result.errorMessage());
    }
}

