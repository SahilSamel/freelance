package com.freelance.tests.configuration;

import com.freelance.configuration.dto.error.list.ErrorConfigurationListRequestDTO;
import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateRequestDTO;
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
@DisplayName("Stage 04.4: Customer Error Configuration Management & RBAC")
@TestMethodOrder(OrderAnnotation.class)
class ErrorConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("1. config_officer: list and update error configurations successfully")
    void testErrorConfigurationLifecycle() {
        String token = getConfigOfficerToken();

        // 1. List error configurations
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(ErrorConfigurationListRequestDTO.empty())
                .when()
                .post("/configurations/error/list")
                .then()
                .statusCode(200)
                .body("groups", notNullValue())
                .body("groups.size()", greaterThan(0))
                .body("totalCount", greaterThan(0));

        String errorKey = "framework.VALIDATION_ERROR";

        // 2. Update error configuration
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new ErrorConfigurationUpdateRequestDTO(errorKey, 400, "Updated custom error message", "Updated desc"))
                .when()
                .post("/configurations/error/update")
                .then()
                .statusCode(200)
                .body("errorId", equalTo(errorKey))
                .body("errorMessage", equalTo("Updated custom error message"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Update non-existent error key returns 404 (FL_6102)")
    void updateErrorConfiguration_nonExistentKey_returns400() {
        String token = getConfigOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new ErrorConfigurationUpdateRequestDTO("non.existent.error.key", 400, "msg", "desc"))
                .when()
                .post("/configurations/error/update")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("FL_6102"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Update error config with blank key returns 400 validation error (FL_1001)")
    void updateErrorConfiguration_blankKey_returns400() {
        String token = getConfigOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new ErrorConfigurationUpdateRequestDTO("", 400, "msg", "desc"))
                .when()
                .post("/configurations/error/update")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Cross-boundary RBAC: user_officer cannot update error configurations (403 FL_4101)")
    void rbac_userOfficerCannotUpdateErrorConfig() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new ErrorConfigurationUpdateRequestDTO("framework.VALIDATION_ERROR", 400, "Hacked", "desc"))
                .when()
                .post("/configurations/error/update")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_4101"));
    }
}
