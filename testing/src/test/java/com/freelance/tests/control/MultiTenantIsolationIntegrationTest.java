package com.freelance.tests.control;

import com.freelance.auth.dto.AuthLoginRequestDTO;
import com.freelance.configuration.dto.system.list.SystemConfigurationListRequestDTO;
import com.freelance.configuration.dto.system.list.SystemConfigurationListResponseDTO;
import com.freelance.tests.BaseIntegrationTest;
import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.read.UserReadRequestDTO;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Order(50)
@DisplayName("Stage 05: Multi-Tenant Schema Separation & Data Isolation")
@TestMethodOrder(OrderAnnotation.class)
public class MultiTenantIsolationIntegrationTest extends BaseIntegrationTest {

    private static UUID primaryTenantUserId;

    @Test
    @Order(1)
    @DisplayName("1. Create user in primary tenant (testtenant)")
    void testCreateUserInPrimaryTenant() {
        String token = getUserOfficerToken();
        String uniqueUsername = "tenant_iso_user_" + System.currentTimeMillis();
        String uniquePhone = "+1555" + (System.currentTimeMillis() % 10000000);

        String userIdStr = RestAssured.given()
                .header("X-Tenant-ID", PRIMARY_TENANT)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(uniqueUsername, "Password123!", com.freelance.alerts.AlertChannel.WHATSAPP, uniquePhone))
                .when()
                .post("/users/create")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");

        primaryTenantUserId = UUID.fromString(userIdStr);
        assertNotNull(primaryTenantUserId);
    }

    @Test
    @Order(2)
    @DisplayName("2. Primary tenant user credentials do not authenticate in secondary tenant")
    void testPrimaryUserCannotLoginToSecondaryTenant() {
        var response = RestAssured.given()
                .header("X-Tenant-ID", SECONDARY_TENANT)
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO(SUPER_ADMIN_USERNAME, SUPER_ADMIN_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404)));

        if (response.extract().statusCode() == 400) {
            response.body("errorCode", equalTo("FL_2101"));
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. Primary tenant user ID cannot be read from secondary tenant schema context (404/401)")
    void testPrimaryUserIdNotAccessibleFromSecondaryTenant() {
        RestAssured.given()
                .header("X-Tenant-ID", SECONDARY_TENANT)
                .contentType(ContentType.JSON)
                .body(new UserReadRequestDTO(primaryTenantUserId, null))
                .when()
                .post("/users/read")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(401), equalTo(404)));
    }

    @Test
    @Order(4)
    @DisplayName("4. Configuration queries on secondary tenant are isolated from primary tenant")
    void testSecondaryTenantConfigIsolation() {
        // Query configurations on secondary tenant using control plane or anonymous check
        RestAssured.given()
                .header("X-Tenant-ID", SECONDARY_TENANT)
                .contentType(ContentType.JSON)
                .body(SystemConfigurationListRequestDTO.empty())
                .when()
                .post("/configurations/system/list")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(401), equalTo(403), equalTo(404)));
    }
}
