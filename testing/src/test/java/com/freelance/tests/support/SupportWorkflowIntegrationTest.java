package com.freelance.tests.support;

import com.freelance.auth.dto.SupportRequestDTO;
import com.freelance.configuration.dto.system.SystemConfigurationDTO;
import com.freelance.configuration.dto.system.list.SystemConfigurationGroupDTO;
import com.freelance.configuration.dto.system.list.SystemConfigurationListResponseDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.security.KeyPair;
import java.time.Duration;
import java.util.Base64;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Vendor Support Token and Client Access Window workflow:
 * 1. Request access (anonymous)
 * 2. Rejected when window is closed (FL_2105)
 * 3. Grant access (client admin)
 * 4. Check status (active)
 * 5. Vendor god-mode API access (200 OK)
 * 6. Tampered token rejected (FL_2106)
 * 7. Revoke access (client admin)
 * 8. Subsequent vendor requests rejected (FL_2105)
 */
@Order(60)
@DisplayName("Stage 06: Cryptographic Vendor Support (Zero Private Key Exposure)")
@TestMethodOrder(OrderAnnotation.class)
class SupportWorkflowIntegrationTest extends BaseIntegrationTest {

    // Generate ephemeral RSA test key pair
    private static final KeyPair vendorKeyPair = VendorSupportTestHelper.generateRsaKeyPair();

    private static String getVendorSupportToken() {
        return VendorSupportTestHelper.generateSupportToken(
                vendorKeyPair.getPrivate(),
                Duration.ofHours(2)
        );
    }

    private static String originalPublicKey;

    @BeforeAll
    static void configureVendorSupportKey() {
        ensureSystemBootstrapped();
        String adminToken = new SupportWorkflowIntegrationTest().getSuperAdminToken();

        try {
            SystemConfigurationListResponseDTO listResponse = RestAssured
                    .given()
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(ContentType.JSON)
                    .body(com.freelance.configuration.dto.system.list.SystemConfigurationListRequestDTO.empty())
                    .when()
                    .post("/configurations/system/list")
                    .then()
                    .extract()
                    .as(SystemConfigurationListResponseDTO.class);

            originalPublicKey = searchConfigValue(listResponse.groups(), "authentication.vendor.support.publicKey");
        } catch (Exception ignored) {}

        String publicKeyBase64 = Base64.getEncoder().encodeToString(vendorKeyPair.getPublic().getEncoded());

        RestAssured
                .given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(new SystemConfigurationUpdateRequestDTO(
                        "authentication.vendor.support.publicKey",
                        publicKeyBase64,
                        "Integration test vendor public key"
                ))
                .when()
                .post("/configurations/system/update")
                .then()
                .statusCode(200);

        // Ensure support access window is closed initially to test pre-grant rejection
        RestAssured
                .given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/support/revoke-access");
    }

    private static String searchConfigValue(java.util.List<SystemConfigurationGroupDTO> groups, String configId) {
        if (groups == null) return null;
        for (SystemConfigurationGroupDTO g : groups) {
            if (g.configurations() != null) {
                for (SystemConfigurationDTO c : g.configurations()) {
                    if (configId.equals(c.configId())) {
                        return c.configValue();
                    }
                }
            }
            if (g.subGroups() != null) {
                String found = searchConfigValue(g.subGroups(), configId);
                if (found != null) return found;
            }
        }
        return null;
    }

    @org.junit.jupiter.api.AfterAll
    static void restoreVendorSupportKey() {
        try {
            String adminToken = new SupportWorkflowIntegrationTest().getSuperAdminToken();
            if (originalPublicKey != null) {
                RestAssured
                        .given()
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(ContentType.JSON)
                        .body(new SystemConfigurationUpdateRequestDTO(
                                "authentication.vendor.support.publicKey",
                                originalPublicKey,
                                "Restore original vendor public key"
                        ))
                        .when()
                        .post("/configurations/system/update");
            }
            RestAssured
                    .given()
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(ContentType.JSON)
                    .when()
                    .post("/support/grant-access");
        } catch (Exception ignored) {}
    }

    @Test
    @Order(1)
    @DisplayName("1. POST /support/request-access → 200 OK with requested=true")
    void step1_requestAccess_returns200() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new SupportRequestDTO("Database repair needed", "TICKET-INTEGRATION-001"))
                .when()
                .post("/support/request-access")
                .then()
                .statusCode(200)
                .body("requested", equalTo(true));
    }

    @Test
    @Order(2)
    @DisplayName("2. Vendor token call before grant → 403 SUPPORT_ACCESS_DISABLED (FL_2105)")
    void step2_vendorCallWhenWindowClosed_returns400() {
        String token = getVendorSupportToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/audit/list")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_2105"));
    }

    @Test
    @Order(3)
    @DisplayName("3. POST /support/grant-access → 200 OK with active=true")
    void step3_grantAccess_returns200() {
        String adminToken = getSuperAdminToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/support/grant-access")
                .then()
                .statusCode(200)
                .body("active", equalTo(true))
                .body("expiresAt", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("4. POST /support/status → 200 OK with active=true and expiresAt not null")
    void step4_getStatus_returnsActive() {
        String adminToken = getSuperAdminToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/support/status")
                .then()
                .statusCode(200)
                .body("active", equalTo(true))
                .body("expiresAt", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("5. Vendor token call during active window → 200 OK (God-Mode bypasses role checks)")
    void step5_vendorCallDuringActiveWindow_succeeds() {
        String token = getVendorSupportToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/audit/list")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(6)
    @DisplayName("6. Tampered vendor token → 401 INVALID_VENDOR_SUPPORT_TOKEN (FL_2106)")
    void step6_tamperedToken_returns400() {
        String validToken = getVendorSupportToken();
        // Modify payload signature slightly
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "abcde";

        RestAssured
                .given()
                .header("Authorization", "Bearer " + tamperedToken)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/audit/list")
                .then()
                .statusCode(401)
                .body("errorCode", equalTo("FL_2106"));
    }

    @Test
    @Order(7)
    @DisplayName("7. POST /support/revoke-access → 200 OK with revoked=true")
    void step7_revokeAccess_returns200() {
        String adminToken = getSuperAdminToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/support/revoke-access")
                .then()
                .statusCode(200)
                .body("revoked", equalTo(true));
    }

    @Test
    @Order(8)
    @DisplayName("8. Vendor token call after revocation → 403 SUPPORT_ACCESS_DISABLED (FL_2105)")
    void step8_vendorCallAfterRevocation_returns400() {
        String token = getVendorSupportToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/audit/list")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_2105"));
    }
}

