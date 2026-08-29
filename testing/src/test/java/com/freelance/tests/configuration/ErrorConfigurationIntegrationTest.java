package com.freelance.tests.configuration;

import com.freelance.configuration.dto.error.list.ErrorConfigurationListRequestDTO;
import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@DisplayName("Error Configuration Integration Tests")
class ErrorConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("list and update error configuration")
    void testErrorConfigurationLifecycle() {
        String token = loginAsSuperAdmin();

        // 1. List error configurations
        String errorKey = RestAssured
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
                .body("totalCount", greaterThan(0))
                .extract().jsonPath().getString("groups[0].subGroups[0].errorConfigurations[0].errorId");

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
}
