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

        // 1. List configurations
        String configKey = RestAssured
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
                .body("totalCount", greaterThan(0))
                .extract().jsonPath().getString("groups[0].subGroups[0].configurations[0].configId");

        // 2. Update configuration (ensuring >= 32 chars in case JWT secret key is chosen)
        String newValue = "updated_value_test_at_least_32_bytes_long_1234567890!";
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new SystemConfigurationUpdateRequestDTO(configKey, newValue, "Updated via integration test"))
                .when()
                .post("/configurations/system/update")
                .then()
                .statusCode(200)
                .body("configId", equalTo(configKey))
                .body("configValue", equalTo(newValue));
    }
}
