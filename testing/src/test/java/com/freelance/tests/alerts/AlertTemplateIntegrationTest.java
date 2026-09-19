package com.freelance.tests.alerts;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.dto.*;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

@DisplayName("Alerts & Template Management Integration Tests")
class AlertTemplateIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("alerts_officer: full template lifecycle (list attributes -> list categories -> create -> read -> update -> list)")
    void fullTemplateLifecycle_success() {
        String token = getAlertsOfficerToken();

        // 1. List whitelisted attributes
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .post("/alerts/attributes/whitelisted")
                .then()
                .statusCode(200)
                .body("attributes", notNullValue());

        // 2. List categories
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .post("/alerts/templates/categories")
                .then()
                .statusCode(200)
                .body("categories", notNullValue());

        // 3. Create Template
        String templateKey = "TMPL_IT_" + System.currentTimeMillis();
        TemplateCreateRequestDTO createReq = new TemplateCreateRequestDTO(
                templateKey,
                "Integration Template",
                "UTILITY",
                "en",
                "Hello {{user.name}}, your code is 1234",
                null,
                null,
                AlertChannel.WHATSAPP,
                Set.of("user.name"),
                List.of(new TemplateButtonDTO("Confirm", ButtonType.QUICK_REPLY, "CONFIRM_" + System.currentTimeMillis(), null, true, 1))
        );

        String templateIdStr = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createReq)
                .when()
                .post("/alerts/templates/create")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("templateKey", equalTo(templateKey))
                .extract()
                .jsonPath()
                .getString("id");

        UUID templateId = UUID.fromString(templateIdStr);

        // 4. Read Template
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new TemplateReadRequestDTO(templateId))
                .when()
                .post("/alerts/templates/read")
                .then()
                .statusCode(200)
                .body("id", equalTo(templateIdStr))
                .body("templateKey", equalTo(templateKey));

        // 5. Update Template
        TemplateUpdateRequestDTO updateReq = new TemplateUpdateRequestDTO(
                templateId,
                templateKey,
                "Updated Template Name",
                "MARKETING",
                "en",
                "Updated Content {{user.name}}",
                null,
                null,
                Set.of("user.name"),
                List.of(new TemplateButtonDTO("Acknowledge", ButtonType.QUICK_REPLY, "ACK_" + System.currentTimeMillis(), null, true, 1))
        );

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(updateReq)
                .when()
                .post("/alerts/templates/update")
                .then()
                .statusCode(200)
                .body("id", equalTo(templateIdStr))
                .body("name", equalTo("Updated Template Name"));

        // 6. List Templates
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new TemplateListRequestDTO(null, null))
                .when()
                .post("/alerts/templates/list")
                .then()
                .statusCode(200)
                .body("templates", notNullValue())
                .body("templates.size()", greaterThan(0));
    }

    @Test
    @DisplayName("read non-existent template returns 200 with empty body")
    void readTemplate_nonExistent_returnsEmpty() {
        String token = getAlertsOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new TemplateReadRequestDTO(UUID.randomUUID()))
                .when()
                .post("/alerts/templates/read")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("create template with blank key returns 400 validation error (FL_1001)")
    void createTemplate_blankKey_returns400() {
        String token = getAlertsOfficerToken();

        TemplateCreateRequestDTO invalidReq = new TemplateCreateRequestDTO(
                "",
                "Name",
                "UTILITY",
                "en",
                "Content",
                null,
                null,
                AlertChannel.WHATSAPP,
                Set.of(),
                List.of()
        );

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(invalidReq)
                .when()
                .post("/alerts/templates/create")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    // ── Cross-boundary RBAC Security Tests ────────────────────────────────────

    @Test
    @DisplayName("cross-boundary RBAC: user_officer cannot create alert templates (403 FL_4101)")
    void rbac_userOfficerCannotCreateTemplate() {
        String token = getUserOfficerToken();

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "UNAUTHORIZED_TMPL",
                "Name",
                "UTILITY",
                "en",
                "Content",
                null,
                null,
                AlertChannel.WHATSAPP,
                Set.of(),
                List.of()
        );

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/alerts/templates/create")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_4101"));
    }
}
