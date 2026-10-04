package com.freelance.tests.configuration;

import com.freelance.configuration.dto.system.list.SystemConfigurationListRequestDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.*;

@Order(44)
@DisplayName("Stage 04.4: Customer System Configuration Management & RBAC")
@TestMethodOrder(OrderAnnotation.class)
class SystemConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("1. config_officer: list and update system configurations successfully")
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
    @Order(2)
    @DisplayName("2. Update non-existent system config key returns 404 (FL_6101)")
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
    @Order(3)
    @DisplayName("3. Update system config with blank key returns 400 validation error (FL_1001)")
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

    @Test
    @Order(4)
    @DisplayName("4. Cross-boundary RBAC: audit_officer cannot update system configurations (403 FL_4101)")
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

    @Test
    @Order(5)
    @DisplayName("5. Anonymous get configurations via POST and GET /configurations/system/get")
    void testGetConfigurationsAnonymous() {
        // POST /configurations/system/get
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new com.freelance.configuration.dto.system.get.SystemConfigurationGetRequestDTO(
                        java.util.List.of("authentication.concurrentLoginAllowed", "non.existent.key")
                ))
                .when()
                .post("/configurations/system/get")
                .then()
                .statusCode(200)
                .body("values.'authentication.concurrentLoginAllowed'", notNullValue());

        // GET /configurations/system/get?keys=...
        RestAssured
                .given()
                .queryParam("keys", "authentication.concurrentLoginAllowed,non.existent.key")
                .when()
                .get("/configurations/system/get")
                .then()
                .statusCode(200)
                .body("values.'authentication.concurrentLoginAllowed'", notNullValue());
    }
}
