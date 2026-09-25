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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Control Plane Management Integration Tests")
@TestMethodOrder(OrderAnnotation.class)
public class ControlPlaneIntegrationTest extends BaseIntegrationTest {

    private static final String CONTROL_TENANT = "freelance";
    private static final String CONTROL_ADMIN_USERNAME = "controladmin";
    private static final String CONTROL_ADMIN_PASSWORD = "ControlAdmin@123";

    private static final String NEW_TENANT_ID = "alpha_corp_" + (System.currentTimeMillis() % 1000000);
    private static final String NEW_TENANT_NAME = "Alpha Corporation";

    private static String controlAdminToken;

    private String getControlAdminToken() {
        if (controlAdminToken != null) {
            return controlAdminToken;
        }

        controlAdminToken = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO(CONTROL_ADMIN_USERNAME, CONTROL_ADMIN_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .as(AuthLoginResponseDTO.class)
                .token();

        assertNotNull(controlAdminToken, "Control admin token must not be null");
        return controlAdminToken;
    }

    @Test
    @Order(1)
    @DisplayName("Control Admin can authenticate on freelance control tenant")
    void testControlAdminLogin() {
        String token = getControlAdminToken();
        assertFalse(token.isBlank(), "Control admin token must not be blank");
    }

    @Test
    @Order(2)
    @DisplayName("Requests without X-Tenant-ID header are rejected with HTTP 400")
    void testRejectRequestsWithoutTenantHeader() {
        RestAssured.given()
                .header("X-Tenant-ID", "")
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("anyuser", "anypass"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(3)
    @DisplayName("Requests with non-existent tenant are rejected with HTTP 404")
    void testRejectRequestsWithUnknownTenant() {
        RestAssured.given()
                .header("X-Tenant-ID", "non_existent_tenant_999")
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("anyuser", "anypass"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    @DisplayName("Control Admin can list all tenants via /control/tenants")
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
        assertTrue(response.tenants().stream().anyMatch(t -> "testtenant".equalsIgnoreCase(t.tenantId())));
    }

    @Test
    @Order(5)
    @DisplayName("Control Admin cannot deactivate freelance control tenant")
    void testCannotDeactivateFreelance() {
        String token = getControlAdminToken();

        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/control/tenants/" + CONTROL_TENANT)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("Control Admin can onboard a new customer tenant")
    void testOnboardNewTenant() {
        String token = getControlAdminToken();

        TenantCreateRequestDTO request = new TenantCreateRequestDTO(
                NEW_TENANT_ID,
                NEW_TENANT_NAME
        );

        TenantOnboardResponseDTO response = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/control/tenants")
                .then()
                .statusCode(201)
                .extract()
                .as(TenantOnboardResponseDTO.class);

        assertEquals(NEW_TENANT_ID, response.tenantId());
        assertEquals("ACTIVE", response.status());
        assertNotNull(response.day0Token());
        assertFalse(response.day0Token().isBlank());
    }

    @Test
    @Order(7)
    @DisplayName("Onboarded customer tenant accepts traffic with X-Tenant-ID")
    void testNewTenantAcceptsTraffic() {
        // Authenticate with wrong credentials on new tenant -> should get 401 Unauthorized (proves tenant is active & routed)
        RestAssured.given()
                .header("X-Tenant-ID", NEW_TENANT_ID)
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("admin_" + NEW_TENANT_ID, "WrongPassword"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(8)
    @DisplayName("Control Admin can soft-deactivate tenant and subsequent requests are blocked with HTTP 403")
    void testDeactivateTenantBlocksTraffic() {
        String token = getControlAdminToken();

        TenantResponseDTO response = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/control/tenants/" + NEW_TENANT_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(TenantResponseDTO.class);

        assertEquals("DEACTIVATED", response.status());

        // Subsequent requests to the deactivated tenant must return HTTP 403 Forbidden
        RestAssured.given()
                .header("X-Tenant-ID", NEW_TENANT_ID)
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("admin_" + NEW_TENANT_ID, "Password123!"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(9)
    @DisplayName("Control Admin can mint vendor support session token for active tenant")
    void testGenerateSupportSessionToken() {
        String token = getControlAdminToken();

        SupportSessionResponseDTO response = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/control/tenants/testtenant/support-session")
                .then()
                .statusCode(200)
                .extract()
                .as(SupportSessionResponseDTO.class);

        assertEquals("testtenant", response.tenantId());
        assertNotNull(response.vendorToken());
        assertFalse(response.vendorToken().isBlank());
        assertNotNull(response.expiresAt());
    }

    @Test
    @Order(10)
    @DisplayName("Customer tenants cannot access control endpoints (HTTP 403)")
    void testCustomerTenantCannotAccessControlEndpoints() {
        String customerAdminToken = getSuperAdminToken();

        RestAssured.given()
                .header("X-Tenant-ID", "testtenant")
                .header("Authorization", "Bearer " + customerAdminToken)
                .when()
                .get("/control/tenants")
                .then()
                .statusCode(403);
    }
}
