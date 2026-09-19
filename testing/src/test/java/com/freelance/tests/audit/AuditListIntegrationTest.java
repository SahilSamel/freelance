package com.freelance.tests.audit;

import com.freelance.audit.dto.list.AuditLogListRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@DisplayName("Audit List Integration & RBAC Tests")
class AuditListIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("audit_officer: queries audit logs successfully (200 OK)")
    void listAuditLogs_auditOfficer_returns200() {
        String token = getAuditOfficerToken();

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
    @DisplayName("audit logs contain sanitized requestData with masked password")
    void listAuditLogs_containsSanitizedRequestData() {
        String token = getAuditOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(AuditLogListRequestDTO.empty())
                .when()
                .post("/audit/list")
                .then()
                .statusCode(200)
                .body("auditLogs.find { it.endpoint == '/auth/login' }.requestData", containsString("\"password\":\"******\""));
    }

    @Test
    @DisplayName("cross-boundary RBAC: user_officer cannot list audit logs (403 FL_4101)")
    void listAuditLogs_userOfficer_forbidden() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(AuditLogListRequestDTO.empty())
                .when()
                .post("/audit/list")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_4101"));
    }

    @Test
    @DisplayName("unauthorized request without token fails (400 or 401)")
    void listAuditLogs_noToken_fails() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(AuditLogListRequestDTO.empty())
                .when()
                .post("/audit/list")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(401)));
    }
}

