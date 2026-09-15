package com.freelance.tests.audit;

import com.freelance.audit.dto.list.AuditLogListRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@DisplayName("Audit List Integration Tests")
class AuditListIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("valid token with SUPER_ADMIN role returns 200 and audit logs")
    void listAuditLogs_superAdmin_returns200() {
        String token = loginAsSuperAdmin();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(AuditLogListRequestDTO.empty())
                .when()
                .post("/audit/list")
                .then()
                .statusCode(200)
                .body("auditLogs", notNullValue())
                .body("totalCount", notNullValue());
    }

    @Test
    @DisplayName("unauthorized request without token returns 400")
    void listAuditLogs_noToken_returns400() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(AuditLogListRequestDTO.empty())
                .when()
                .post("/audit/list")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("audit logs contain sanitized requestData with masked password")
    void listAuditLogs_containsSanitizedRequestData() {
        String token = loginAsSuperAdmin();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(AuditLogListRequestDTO.empty())
                .when()
                .post("/audit/list")
                .then()
                .statusCode(200)
                .body("auditLogs.find { it.endpoint == '/auth/login' }.requestData", containsString("\"password\": \"******\""))
                .body("auditLogs.find { it.endpoint == '/auth/login' }.requestData", containsString("superadmin"))
                .body("auditLogs.find { it.endpoint == '/auth/login' }.requestData", not(containsString("Welcome@1")));
    }
}
