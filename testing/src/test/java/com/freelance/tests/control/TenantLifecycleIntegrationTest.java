package com.freelance.tests.control;

import com.freelance.auth.dto.AuthLoginRequestDTO;
import com.freelance.tenant.dto.TenantResponseDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Order(70)
@DisplayName("Stage 07: Tenant Deactivation & Offboarding Lifecycle")
@TestMethodOrder(OrderAnnotation.class)
public class TenantLifecycleIntegrationTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("1. Control Admin deactivates secondary customer tenant (secondary_tenant)")
    void testDeactivateSecondaryTenant() {
        String token = getControlAdminToken();

        TenantResponseDTO response = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/control/tenants/" + SECONDARY_TENANT)
                .then()
                .statusCode(200)
                .extract()
                .as(TenantResponseDTO.class);

        assertEquals(SECONDARY_TENANT, response.tenantId());
        assertEquals("DEACTIVATED", response.status());
    }

    @Test
    @Order(2)
    @DisplayName("2. Subsequent requests to deactivated tenant are immediately rejected with 404")
    void testDeactivatedTenantTrafficBlocked() {
        RestAssured.given()
                .header("X-Tenant-ID", SECONDARY_TENANT)
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("anyuser", "Password123!"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(3)
    @DisplayName("3. Primary customer tenant (testtenant) remains 100% active and healthy")
    void testPrimaryTenantRemainsActive() {
        RestAssured.given()
                .header("X-Tenant-ID", PRIMARY_TENANT)
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO(SUPER_ADMIN_USERNAME, SUPER_ADMIN_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("4. Cannot deactivate freelance control plane (FL_8102: CANNOT_DEACTIVATE_CONTROL_PLANE)")
    void testCannotDeactivateControlPlane() {
        String token = getControlAdminToken();

        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/control/tenants/" + CONTROL_TENANT)
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_8102"));
    }

    @Test
    @Order(5)
    @DisplayName("5. Deactivating non-existent tenant returns 404 or 400")
    void testDeactivateNonExistentTenantFails() {
        String token = getControlAdminToken();

        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/control/tenants/non_existent_tenant_999")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404)));
    }

    @AfterAll
    public static void flushServerCoverage() {
        try {
            RestAssured.given()
                    .header("X-Tenant-ID", CONTROL_TENANT)
                    .when()
                    .post("/testing/coverage/dump");
        } catch (Exception ignored) {
        }
    }
}
