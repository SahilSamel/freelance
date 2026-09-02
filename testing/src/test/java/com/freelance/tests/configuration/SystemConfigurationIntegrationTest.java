package com.freelance.tests.configuration;

import com.freelance.configuration.dto.system.list.SystemConfigurationListRequestDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@DisplayName("System Configuration Integration Tests")
class SystemConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("list and update system configuration")
    void testSystemConfigurationLifecycle() {
        String token = loginAsSuperAdmin();

        // 1. List configurations and verify validation field is exposed
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
                .body("fieldErrors[0].errorCode", equalTo("FL_6006"))
                .body("fieldErrors[0].fieldName", equalTo("configValue"));

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
    }
}
