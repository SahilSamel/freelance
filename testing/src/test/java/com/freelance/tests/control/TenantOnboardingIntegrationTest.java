package com.freelance.tests.control;

import com.freelance.tenant.dto.TenantCreateRequestDTO;
import com.freelance.tenant.dto.TenantListResponseDTO;
import com.freelance.tenant.dto.TenantOnboardResponseDTO;
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

@Order(20)
@DisplayName("Stage 02: Customer Tenant Onboarding & Schema Migration Pipeline")
@TestMethodOrder(OrderAnnotation.class)
public class TenantOnboardingIntegrationTest extends BaseIntegrationTest {

    private static final String VALID_DYNAMIC_TENANT = "dynamic_corp_" + (System.currentTimeMillis() % 1000000);

    @Test
    @Order(1)
    @DisplayName("1. Reject onboarding with blank tenant ID (400 FL_1001)")
    void testRejectBlankTenantId() {
        String token = getControlAdminToken();

        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new TenantCreateRequestDTO("", "Blank Tenant"))
                .when()
                .post("/control/tenants")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Reject onboarding with short tenant ID (<2 chars) (400 FL_1001)")
    void testRejectShortTenantId() {
        String token = getControlAdminToken();

        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new TenantCreateRequestDTO("a", "Short Tenant"))
                .when()
                .post("/control/tenants")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Reject onboarding with invalid special characters (400 FL_1001)")
    void testRejectInvalidCharacters() {
        String token = getControlAdminToken();

        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new TenantCreateRequestDTO("invalid@#$name", "Invalid Characters"))
                .when()
                .post("/control/tenants")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Reject duplicate onboarding of existing tenant (409 FL_8101)")
    void testRejectDuplicateOnboarding() {
        String token = getControlAdminToken();

        RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new TenantCreateRequestDTO(PRIMARY_TENANT, "Duplicate Primary Tenant"))
                .when()
                .post("/control/tenants")
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("FL_8101"));
    }

    @Test
    @Order(5)
    @DisplayName("5. Onboard secondary customer tenant (secondary_tenant) successfully")
    void testOnboardSecondaryTenant() {
        String token = getControlAdminToken();

        // Check if secondary_tenant already exists from previous runs
        TenantListResponseDTO listRes = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/control/tenants")
                .then()
                .statusCode(200)
                .extract()
                .as(TenantListResponseDTO.class);

        boolean exists = listRes.tenants().stream().anyMatch(t -> SECONDARY_TENANT.equalsIgnoreCase(t.tenantId()));
        if (!exists) {
            TenantOnboardResponseDTO response = RestAssured.given()
                    .header("X-Tenant-ID", CONTROL_TENANT)
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(new TenantCreateRequestDTO(SECONDARY_TENANT, "Globex Secondary Industries"))
                    .when()
                    .post("/control/tenants")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TenantOnboardResponseDTO.class);

            assertEquals(SECONDARY_TENANT, response.tenantId());
            assertEquals("ACTIVE", response.status());
            assertNotNull(response.day0Token());
            assertFalse(response.day0Token().isBlank());
        }
    }

    @Test
    @Order(6)
    @DisplayName("6. Onboard dynamic customer tenant with Flyway migration and Day-0 Token")
    void testOnboardDynamicCustomerTenant() {
        String token = getControlAdminToken();

        TenantOnboardResponseDTO response = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new TenantCreateRequestDTO(VALID_DYNAMIC_TENANT, "Dynamic Corporation"))
                .when()
                .post("/control/tenants")
                .then()
                .statusCode(201)
                .extract()
                .as(TenantOnboardResponseDTO.class);

        assertEquals(VALID_DYNAMIC_TENANT, response.tenantId());
        assertEquals("ACTIVE", response.status());
        assertNotNull(response.day0Token());
        assertFalse(response.day0Token().isBlank());
        assertNotNull(response.publicKeyBase64());

        // Verify the new tenant is listed in tenant registry
        TenantListResponseDTO listResponse = RestAssured.given()
                .header("X-Tenant-ID", CONTROL_TENANT)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/control/tenants")
                .then()
                .statusCode(200)
                .extract()
                .as(TenantListResponseDTO.class);

        assertTrue(listResponse.tenants().stream().anyMatch(t -> VALID_DYNAMIC_TENANT.equalsIgnoreCase(t.tenantId())));
    }
}
