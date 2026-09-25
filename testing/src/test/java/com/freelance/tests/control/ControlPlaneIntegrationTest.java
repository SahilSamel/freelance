package com.freelance.tests.control;

import com.freelance.auth.dto.AuthLoginRequestDTO;
import com.freelance.auth.dto.AuthLoginResponseDTO;
import com.freelance.tenant.dto.SupportSessionResponseDTO;
import com.freelance.tenant.dto.TenantCreateRequestDTO;
import com.freelance.tenant.dto.TenantListResponseDTO;
import com.freelance.tenant.dto.TenantOnboardResponseDTO;
import com.freelance.tenant.dto.TenantResponseDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Order(10)
@DisplayName("Stage 01: Pre-Tenant Control Plane Baseline & Adversarial Security")
@TestMethodOrder(OrderAnnotation.class)
public class ControlPlaneIntegrationTest extends BaseIntegrationTest {

    private static final String DUMMY_TENANT_ID = "temp_dummy_" + (System.currentTimeMillis() % 100000);

    @Test
    @Order(1)
    @DisplayName("1. Control Admin authenticates successfully on freelance control tenant")
    void testControlAdminLogin() {
        String token = getControlAdminToken();
        assertNotNull(token, "Control admin token must not be null");
        assertFalse(token.isBlank(), "Control admin token must not be blank");
    }

    @Test
    @Order(2)
    @DisplayName("2. Control Admin login fails with wrong password (FL_2101)")
    void testControlAdminLoginWrongPassword() {
        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO(CONTROL_ADMIN_USERNAME, "WrongPass123!"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_2101"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Requests with missing or blank X-Tenant-ID header are rejected with 404 (anti-enumeration)")
    void testRejectRequestsWithoutTenantHeader() {
        RestAssured.given()
                .header("X-Tenant-ID", "")
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("anyuser", "anypass"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    @DisplayName("4. Requests with unknown or invalid tenant regex return 404 (anti-enumeration)")
    void testRejectRequestsWithUnknownTenant() {
        RestAssured.given()
                .header("X-Tenant-ID", "non_existent_tenant_9999")
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("anyuser", "anypass"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(404);

        RestAssured.given()
                .header("X-Tenant-ID", "bad@tenant#name!")
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("anyuser", "anypass"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(5)
    @DisplayName("5. Calling customer API on non-existent tenant returns 404")
    void testCustomerEndpointOnNonExistentTenantFails() {
        RestAssured.given()
                .header("X-Tenant-ID", "unknown_customer_tenant")
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/users/read")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    @DisplayName("6. Unauthenticated caller cannot access /control/tenants (400 / 401 / 403 / 404)")
    void testUnauthenticatedControlAccessFails() {
        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .when()
                .get("/control/tenants")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(401), equalTo(403), equalTo(404)));
    }

    @Test
    @Order(7)
    @DisplayName("7. Customer tenant token cannot access /control/tenants (400 / 401 / 403 / 404)")
    void testCustomerTenantTokenCannotAccessControlPlane() {
        String customerToken = getSuperAdminToken();

        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + customerToken)
                .when()
                .get("/control/tenants")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(401), equalTo(403), equalTo(404)));
    }

    @Test
    @Order(8)
    @DisplayName("8. Control Admin can list registered tenants via GET /control/tenants")
    void testListTenants() {
        String token = getControlAdminToken();

        TenantListResponseDTO response = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/control/tenants")
                .then()
                .statusCode(200)
                .extract()
                .as(TenantListResponseDTO.class);

        assertNotNull(response.tenants());
        assertTrue(response.tenants().stream().anyMatch(t -> CONTROL_TENANT.equalsIgnoreCase(t.tenantId())));
    }

    @Test
    @Order(9)
    @DisplayName("9. Cannot deactivate the freelance control plane (FL_8102: CANNOT_DEACTIVATE_CONTROL_PLANE)")
    void testCannotDeactivateFreelance() {
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
    @Order(10)
    @DisplayName("10. Attempting support session for non-existent tenant returns 404 or 400")
    void testSupportSessionForNonExistentTenantFails() {
        String token = getControlAdminToken();

        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .post("/control/tenants/non_existent_tenant_xyz/support-session")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404)));
    }

    @Test
    @Order(11)
    @DisplayName("11. Lifecycle Mutation: create dummy tenant and immediately deactivate it")
    void testCreateAndDeactivateDummyTenant() {
        String token = getControlAdminToken();

        // 1. Create temporary dummy tenant
        TenantOnboardResponseDTO onboardRes = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new TenantCreateRequestDTO(DUMMY_TENANT_ID, "Dummy Temporary Tenant"))
                .when()
                .post("/control/tenants")
                .then()
                .statusCode(201)
                .extract()
                .as(TenantOnboardResponseDTO.class);

        assertEquals(DUMMY_TENANT_ID, onboardRes.tenantId());
        assertEquals("ACTIVE", onboardRes.status());

        // 2. Soft-deactivate dummy tenant
        TenantResponseDTO deactRes = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/control/tenants/" + DUMMY_TENANT_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(TenantResponseDTO.class);

        assertEquals("DEACTIVATED", deactRes.status());

        // 3. Assert immediate traffic cutoff (404 Not Found)
        RestAssured.given()
                .header("X-Tenant-ID", DUMMY_TENANT_ID)
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("admin", "pass"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(404);
    }
}
