package com.freelance.tests.bootstrap;

import com.freelance.auth.dto.AuthLoginRequestDTO;
import com.freelance.authorization.dto.AuthorizationDTO;
import com.freelance.authorization.dto.list.AuthorizationListResponseDTO;
import com.freelance.authorization.dto.role.create.RoleCreateRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import com.freelance.user.create.UserCreateRequestDTO;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Day-0 System Bootstrap & Persona Provisioning Journey")
@TestMethodOrder(OrderAnnotation.class)
class Day0BootstrapIntegrationTest extends BaseIntegrationTest {

    private static final String RUN_ID = UUID.randomUUID().toString().substring(0, 8);
    private static final String SUPER_ADMIN_ROLE = "DAY0_ROLE_SUPER_ADMIN_" + RUN_ID;
    private static final String USER_ADMIN_ROLE = "DAY0_ROLE_USER_ADMIN_" + RUN_ID;
    private static final String ROLE_ADMIN_ROLE = "DAY0_ROLE_ROLE_ADMIN_" + RUN_ID;
    private static final String CONFIG_ADMIN_ROLE = "DAY0_ROLE_CONFIG_ADMIN_" + RUN_ID;
    private static final String ALERTS_ADMIN_ROLE = "DAY0_ROLE_ALERTS_ADMIN_" + RUN_ID;
    private static final String AUDIT_VIEWER_ROLE = "DAY0_ROLE_AUDIT_VIEWER_" + RUN_ID;

    private static final String USER_SUPER_ADMIN = "day0_super_" + RUN_ID;
    private static final String USER_USER_OFFICER = "day0_user_" + RUN_ID;
    private static final String USER_ROLE_OFFICER = "day0_role_" + RUN_ID;
    private static final String USER_CONFIG_OFFICER = "day0_config_" + RUN_ID;
    private static final String USER_ALERTS_OFFICER = "day0_alerts_" + RUN_ID;
    private static final String USER_AUDIT_OFFICER = "day0_audit_" + RUN_ID;

    private static String day0Token;
    private static List<AuthorizationDTO> allAuthorizations;
    private static UUID superAdminRoleId;
    private static UUID userAdminRoleId;
    private static UUID roleAdminRoleId;
    private static UUID configAdminRoleId;
    private static UUID alertsAdminRoleId;
    private static UUID auditViewerRoleId;

    @org.junit.jupiter.api.BeforeAll
    public static void initDay0Environment() {
        day0Token = getDay0VendorToken();
        if (day0Token == null) return;

        try {
            String pubKeyBase64 = null;
            if (com.freelance.initialization.Init.getDefaultTenantResult() != null) {
                pubKeyBase64 = com.freelance.initialization.Init.getDefaultTenantResult().publicKeyBase64();
            }
            if (pubKeyBase64 == null) {
                try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/freelance_db", "postgres", "welcome1");
                     java.sql.PreparedStatement ps = conn.prepareStatement(
                        "SELECT config_value FROM freelance.FL_SYSTEM_CONFIGURATION WHERE config_key = 'platform.vendor.publicKey'");
                     java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        pubKeyBase64 = rs.getString(1);
                    }
                } catch (Exception ignored) {}
            }

            if (pubKeyBase64 != null && !pubKeyBase64.isBlank()) {
                String adminToken = new Day0BootstrapIntegrationTest().getSuperAdminToken();
                RestAssured.given()
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(ContentType.JSON)
                        .body(new com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO(
                                "authentication.vendor.support.publicKey",
                                pubKeyBase64,
                                "Ensure Day0 public key is active for Day0 tests"
                        ))
                        .when()
                        .post("/configurations/system/update");

                RestAssured.given()
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(ContentType.JSON)
                        .when()
                        .post("/support/grant-access");
            }
        } catch (Exception ignored) {}
    }

    @org.junit.jupiter.api.AfterAll
    public static void restoreAccessAfterDay0Tests() {
        try {
            String adminToken = new Day0BootstrapIntegrationTest().getSuperAdminToken();
            RestAssured.given()
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(ContentType.JSON)
                    .when()
                    .post("/support/grant-access");
        } catch (Exception ignored) {}
    }

    @Test
    @Order(1)
    @DisplayName("1. Day-0 Vendor Support token is available from bootstrap ceremony")
    void step1_verifyDay0TokenAvailable() {
        day0Token = getDay0VendorToken();
        assertNotNull(day0Token, "Day-0 vendor support token must be available from startup bootstrap ceremony");
        assertFalse(day0Token.isBlank(), "Day-0 vendor token must not be blank");
    }

    @Test
    @Order(2)
    @DisplayName("2. Day-0 token queries authorization registry (POST /authorizations/list)")
    void step2_fetchAuthorizationsWithDay0Token() {
        AuthorizationListResponseDTO response = RestAssured
                .given()
                .header("Authorization", "Bearer " + day0Token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/authorizations/list")
                .then()
                .statusCode(200)
                .extract()
                .as(AuthorizationListResponseDTO.class);

        assertNotNull(response);
        allAuthorizations = flattenAuthorizations(response.groups());
        assertFalse(allAuthorizations.isEmpty(), "System authorizations should be discovered and seeded");
        assertTrue(response.totalCount() > 0, "Total authorization count should be greater than 0");
    }

    @Test
    @Order(3)
    @DisplayName("3. Day-0 token creates specialized system roles (POST /roles/create)")
    void step3_createSpecializedRoles() {
        List<UUID> superAdminAuthIds = allAuthorizations.stream().map(AuthorizationDTO::id).toList();
        List<UUID> userAuthIds = allAuthorizations.stream().filter(a -> a.key().startsWith("USER_")).map(AuthorizationDTO::id).toList();
        List<UUID> roleAuthIds = allAuthorizations.stream().filter(a -> a.key().startsWith("ROLE_") || a.key().equals("AUTHORIZATION_LIST")).map(AuthorizationDTO::id).toList();
        List<UUID> configAuthIds = allAuthorizations.stream().filter(a -> a.key().startsWith("SYSTEM_CONFIG_") || a.key().startsWith("ERROR_CONFIG_")).map(AuthorizationDTO::id).toList();
        List<UUID> alertsAuthIds = allAuthorizations.stream().filter(a -> a.key().startsWith("ALERT_")).map(AuthorizationDTO::id).toList();
        List<UUID> auditAuthIds = allAuthorizations.stream().filter(a -> a.key().startsWith("AUDIT_")).map(AuthorizationDTO::id).toList();

        superAdminRoleId = createRole(day0Token, SUPER_ADMIN_ROLE, "Super Administrator Role", superAdminAuthIds);
        userAdminRoleId = createRole(day0Token, USER_ADMIN_ROLE, "User Officer Role", userAuthIds);
        roleAdminRoleId = createRole(day0Token, ROLE_ADMIN_ROLE, "Role Officer Role", roleAuthIds);
        configAdminRoleId = createRole(day0Token, CONFIG_ADMIN_ROLE, "Config Officer Role", configAuthIds);
        alertsAdminRoleId = createRole(day0Token, ALERTS_ADMIN_ROLE, "Alerts Officer Role", alertsAuthIds);
        auditViewerRoleId = createRole(day0Token, AUDIT_VIEWER_ROLE, "Audit Viewer Role", auditAuthIds);

        assertNotNull(superAdminRoleId);
        assertNotNull(userAdminRoleId);
        assertNotNull(roleAdminRoleId);
        assertNotNull(configAdminRoleId);
        assertNotNull(alertsAdminRoleId);
        assertNotNull(auditViewerRoleId);
    }

    @Test
    @Order(4)
    @DisplayName("4. Day-0 token provisions dedicated persona users (POST /users/create)")
    void step4_provisionPersonaUsers() {
        createPersonaUser(day0Token, USER_SUPER_ADMIN, "Day0SuperAdminPass123!", List.of(superAdminRoleId));
        createPersonaUser(day0Token, USER_USER_OFFICER, "Day0UserOfficerPass123!", List.of(userAdminRoleId));
        createPersonaUser(day0Token, USER_ROLE_OFFICER, "Day0RoleOfficerPass123!", List.of(roleAdminRoleId));
        createPersonaUser(day0Token, USER_CONFIG_OFFICER, "Day0ConfigOfficerPass123!", List.of(configAdminRoleId));
        createPersonaUser(day0Token, USER_ALERTS_OFFICER, "Day0AlertsOfficerPass123!", List.of(alertsAdminRoleId));
        createPersonaUser(day0Token, USER_AUDIT_OFFICER, "Day0AuditOfficerPass123!", List.of(auditViewerRoleId));
    }

    @Test
    @Order(5)
    @DisplayName("5. Revoke Day-0 access window (POST /support/revoke-access)")
    void step5_revokeDay0Access() {
        RestAssured
                .given()
                .header("Authorization", "Bearer " + day0Token)
                .contentType(ContentType.JSON)
                .when()
                .post("/support/revoke-access")
                .then()
                .statusCode(200)
                .body("revoked", equalTo(true));
    }

    @Test
    @Order(6)
    @DisplayName("6. Day-0 token is rejected after revocation (403 FL_2105)")
    void step6_verifyDay0TokenRejected() {
        RestAssured
                .given()
                .header("Authorization", "Bearer " + day0Token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/audit/list")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_2105"));
    }

    @Test
    @Order(7)
    @DisplayName("7. All newly provisioned personas can authenticate (POST /auth/login)")
    void step7_verifyAllPersonasCanLogin() {
        verifyLogin(USER_SUPER_ADMIN, "Day0SuperAdminPass123!");
        verifyLogin(USER_USER_OFFICER, "Day0UserOfficerPass123!");
        verifyLogin(USER_ROLE_OFFICER, "Day0RoleOfficerPass123!");
        verifyLogin(USER_CONFIG_OFFICER, "Day0ConfigOfficerPass123!");
        verifyLogin(USER_ALERTS_OFFICER, "Day0AlertsOfficerPass123!");
        verifyLogin(USER_AUDIT_OFFICER, "Day0AuditOfficerPass123!");
    }

    private UUID createRole(String token, String name, String description, List<UUID> authIds) {
        String idStr = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleCreateRequestDTO(name, description, authIds))
                .when()
                .post("/roles/create")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo(name))
                .extract()
                .jsonPath()
                .getString("id");

        return UUID.fromString(idStr);
    }

    private void createPersonaUser(String token, String username, String password, List<UUID> roleIds) {
        String phone = "+1888" + String.format("%07d", Math.abs(username.hashCode()) % 10000000);
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(username, password, roleIds, com.freelance.alerts.AlertChannel.WHATSAPP, phone))
                .when()
                .post("/users/create")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("username", equalTo(username));
    }

    private void verifyLogin(String username, String password) {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO(username, password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }
}


