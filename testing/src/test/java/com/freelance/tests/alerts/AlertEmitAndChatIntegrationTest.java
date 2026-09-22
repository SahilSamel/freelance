package com.freelance.tests.alerts;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.dto.*;
import com.freelance.alerts.dto.webhook.*;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import com.freelance.user.create.UserCreateRequestDTO;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

@DisplayName("Alert Emit, Chat Timeline & Webhook Integration Tests")
class AlertEmitAndChatIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("alerts_officer: emit alert batch, view chat timeline, send staff reply")
    void alertEmitAndChatLifecycle_success() {
        String alertsToken = getAlertsOfficerToken();
        String userToken = getUserOfficerToken();

        // 1. Create a target user
        String targetUsername = "alert_target_" + System.currentTimeMillis();
        String targetUserIdStr = RestAssured
                .given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(targetUsername, "SecureTargetPass123!", AlertChannel.WHATSAPP, "+919876543210"))
                .when()
                .post("/users/create")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");

        UUID targetUserId = UUID.fromString(targetUserIdStr);

        // 2. Create a template for emitting
        String templateKey = "EMIT_TMPL_" + System.currentTimeMillis();
        TemplateCreateRequestDTO createReq = new TemplateCreateRequestDTO(
                templateKey,
                "Emit Template",
                "UTILITY",
                "en",
                "Hello user, please acknowledge notification.",
                null,
                null,
                AlertChannel.WHATSAPP,
                Set.of(),
                List.of()
        );

        String templateIdStr = RestAssured
                .given()
                .header("Authorization", "Bearer " + alertsToken)
                .contentType(ContentType.JSON)
                .body(createReq)
                .when()
                .post("/alerts/templates/create")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");

        UUID templateId = UUID.fromString(templateIdStr);

        // 3. Get an approved template for emitting (only approved templates can be dispatched)
        TemplateListResponseDTO tplList = RestAssured
                .given()
                .header("Authorization", "Bearer " + alertsToken)
                .contentType(ContentType.JSON)
                .body(new TemplateListRequestDTO(null, com.freelance.alerts.TemplateStatus.APPROVED))
                .when()
                .post("/alerts/templates/list")
                .then()
                .statusCode(200)
                .extract()
                .as(TemplateListResponseDTO.class);

        UUID approvedTemplateId = tplList.templates().get(0).id();

        // Emit alert batch to target user -> 202 Accepted
        RestAssured
                .given()
                .header("Authorization", "Bearer " + alertsToken)
                .contentType(ContentType.JSON)
                .body(new AlertEmitRequestDTO(approvedTemplateId, List.of(targetUserId)))
                .when()
                .post("/alerts/emit")
                .then()
                .statusCode(202);

        // 4. Query chat timeline for target user -> 200 OK
        RestAssured
                .given()
                .header("Authorization", "Bearer " + alertsToken)
                .contentType(ContentType.JSON)
                .body(new ChatTimelineRequestDTO(targetUserId, AlertChannel.WHATSAPP, 50))
                .when()
                .post("/alerts/chat/timeline")
                .then()
                .statusCode(200)
                .body("messages", notNullValue());

        // 5. Send staff chat reply -> 200 OK
        RestAssured
                .given()
                .header("Authorization", "Bearer " + alertsToken)
                .contentType(ContentType.JSON)
                .body(new AdminChatReplyRequestDTO(targetUserId, AlertChannel.WHATSAPP, "Human support agent responding to your inquiry."))
                .when()
                .post("/alerts/chat/reply")
                .then()
                .statusCode(200)
                .body("content", equalTo("Human support agent responding to your inquiry."))
                .body("direction", equalTo("OUTBOUND"));
    }

    @Test
    @DisplayName("meta WhatsApp webhook: GET challenge verification & POST inbound message")
    void whatsappWebhook_lifecycle() {
        String configToken = getConfigOfficerToken();
        String verifyToken = "test_webhook_verify_token_123";

        // 1. Configure webhook verify token
        RestAssured
                .given()
                .header("Authorization", "Bearer " + configToken)
                .contentType(ContentType.JSON)
                .body(new SystemConfigurationUpdateRequestDTO("alerts.whatsapp.webhookVerifyToken", verifyToken, "Webhook token"))
                .when()
                .post("/configurations/system/update")
                .then()
                .statusCode(200);

        // 2. GET verification with matching verify_token -> 200 with challenge
        RestAssured
                .given()
                .queryParam("hub.mode", "subscribe")
                .queryParam("hub.verify_token", verifyToken)
                .queryParam("hub.challenge", "CHALLENGE_STRING_XYZ")
                .when()
                .get("/alerts/webhook/whatsapp")
                .then()
                .statusCode(200)
                .body(equalTo("CHALLENGE_STRING_XYZ"));

        // 3. GET verification with incorrect verify_token -> 403 Forbidden
        RestAssured
                .given()
                .queryParam("hub.mode", "subscribe")
                .queryParam("hub.verify_token", "wrong_token")
                .queryParam("hub.challenge", "CHALLENGE_STRING_XYZ")
                .when()
                .get("/alerts/webhook/whatsapp")
                .then()
                .statusCode(403);

        // 4. POST inbound payload -> 200 EVENT_RECEIVED
        WhatsAppWebhookPayloadDTO payload = new WhatsAppWebhookPayloadDTO(
                "whatsapp_business_account",
                List.of(new WebhookEntryDTO(
                        "entry_id_001",
                        List.of(new WebhookChangeDTO(
                                "messages",
                                new WebhookValueDTO(
                                         "whatsapp",
                                         new WebhookMetadataDTO("12345", "67890"),
                                         null,
                                         List.of(new WebhookMessageDTO(
                                                 "919876543210",
                                                 "msg_id_100",
                                                 "1710000000",
                                                 "text",
                                                 new WebhookTextMessageDTO("Hi, I need help with my account"),
                                                 null,
                                                 null
                                         )),
                                         null
                                )
                        ))
                ))
        );

        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/alerts/webhook/whatsapp")
                .then()
                .statusCode(200)
                .body(equalTo("{\"status\":\"EVENT_RECEIVED\"}"));
    }

    // ── Cross-boundary RBAC Security Tests ────────────────────────────────────

    @Test
    @DisplayName("cross-boundary RBAC: user_officer cannot emit alerts (403 FL_4101)")
    void rbac_userOfficerCannotEmitAlerts() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new AlertEmitRequestDTO(UUID.randomUUID(), List.of(UUID.randomUUID())))
                .when()
                .post("/alerts/emit")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_4101"));
    }
}
