package com.freelance.tests.configuration;

import com.freelance.configuration.dto.system.list.SystemConfigurationListRequestDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@DisplayName("System Configuration Integration & RBAC Tests")
class SystemConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("config_officer: list and update system configurations successfully")
    void testSystemConfigurationLifecycle() {
        String token = getConfigOfficerToken();

        // 1. List configurations and verify groups returned
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(SystemConfigurationListRequestDTO.empty())
                .when()
                .post("/configurations/system/list")
                .then()
                .statusCode(200)
                .body("groups", notNullValue())
                .body("groups.size()", greaterThan(0))
                .body("totalCount", greaterThan(0));

        // 2. Test validation failure when candidate value violates key regex
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new SystemConfigurationUpdateRequestDTO("authentication.concurrentLoginAllowed", "invalid_enum_val", "Invalid update"))
                .when()
                .post("/configurations/system/update")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));

        // 3. Test successful update when candidate value matches key regex
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new SystemConfigurationUpdateRequestDTO("authentication.concurrentLoginAllowed", "false", "Valid update"))
                .when()
                .post("/configurations/system/update")
                .then()
                .statusCode(200)
                .body("configId", equalTo("authentication.concurrentLoginAllowed"))
                .body("configValue", equalTo("false"));

        // Revert back
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new SystemConfigurationUpdateRequestDTO("authentication.concurrentLoginAllowed", "true", "Revert to default"))
                .when()
                .post("/configurations/system/update")
                .then()
                .statusCode(200)
                .body("configValue", equalTo("true"));
    }

    @Test
    @DisplayName("update non-existent system config key returns 404 (FL_6101)")
    void updateConfiguration_nonExistentKey_returns400() {
        String token = getConfigOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new SystemConfigurationUpdateRequestDTO("non.existent.config.key", "value", "desc"))
                .when()
                .post("/configurations/system/update")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("FL_6101"));
    }

    @Test
    @DisplayName("update system config with blank key returns 400 validation error (FL_1001)")
    void updateConfiguration_blankKey_returns400() {
        String token = getConfigOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new SystemConfigurationUpdateRequestDTO("", "value", "desc"))
                .when()
                .post("/configurations/system/update")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    // ── Cross-boundary RBAC Security Tests ────────────────────────────────────

    @Test
    @DisplayName("cross-boundary RBAC: audit_officer cannot update system configurations (403 FL_4101)")
    void rbac_auditOfficerCannotUpdateConfig() {
        String token = getAuditOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new SystemConfigurationUpdateRequestDTO("authentication.concurrentLoginAllowed", "false", "Unauthorized update"))
                .when()
                .post("/configurations/system/update")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_4101"));
    }
}

