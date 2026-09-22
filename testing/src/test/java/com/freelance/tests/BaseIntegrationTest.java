package com.freelance.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.authorization.dto.AuthorizationDTO;
import com.freelance.authorization.dto.list.AuthorizationGroupDTO;
import com.freelance.authorization.dto.list.AuthorizationListResponseDTO;
import com.freelance.authorization.dto.role.create.RoleCreateRequestDTO;
import com.freelance.user.create.UserCreateRequestDTO;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseIntegrationTest {

    protected static final String BASE_URL = "https://localhost:8080";

    public static final String SUPER_ADMIN_USERNAME = "super_admin";
    public static final String SUPER_ADMIN_PASSWORD = "SuperAdminPass123!";

    public static final String USER_OFFICER_USERNAME = "user_officer";
    public static final String USER_OFFICER_PASSWORD = "UserOfficerPass123!";

    public static final String ROLE_OFFICER_USERNAME = "role_officer";
    public static final String ROLE_OFFICER_PASSWORD = "RoleOfficerPass123!";

    public static final String CONFIG_OFFICER_USERNAME = "config_officer";
    public static final String CONFIG_OFFICER_PASSWORD = "ConfigOfficerPass123!";

    public static final String ALERTS_OFFICER_USERNAME = "alerts_officer";
    public static final String ALERTS_OFFICER_PASSWORD = "AlertsOfficerPass123!";

    public static final String AUDIT_OFFICER_USERNAME = "audit_officer";
    public static final String AUDIT_OFFICER_PASSWORD = "AuditOfficerPass123!";

    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);

    @BeforeAll
    public static void configureRestAssured() {
        RestAssured.baseURI = BASE_URL;

        String keystorePath = "D:\\programming\\java\\Freelance\\Launcher\\src\\main\\resources\\keystore\\keystore.jks";

        RestAssured.config = RestAssuredConfig.config()
                .sslConfig(SSLConfig.sslConfig()
                        .trustStore(keystorePath, "changeit")
                        .allowAllHostnames())
                .objectMapperConfig(
                        ObjectMapperConfig.objectMapperConfig()
                                .jackson2ObjectMapperFactory((cls, charset) -> new ObjectMapper())
                );

        ensureSystemBootstrapped();
    }

    public static String getDay0VendorToken() {
        try {
            Path tokenPath = Path.of(System.getProperty("java.io.tmpdir"), "freelance-day0-token.jwt");
            if (Files.exists(tokenPath)) {
                return Files.readString(tokenPath).trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static synchronized void ensureSystemBootstrapped() {
        if (BOOTSTRAPPED.get()) {
            return;
        }

        // Check if super_admin can already log in
        try {
            int status = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(new com.freelance.auth.dto.AuthLoginRequestDTO(SUPER_ADMIN_USERNAME, SUPER_ADMIN_PASSWORD))
                    .when()
                    .post("/auth/login")
                    .then()
                    .extract()
                    .statusCode();
            if (status == 200) {
                BOOTSTRAPPED.set(true);
                return;
            }
        } catch (Exception ignored) {}

        String day0Token = getDay0VendorToken();
        if (day0Token == null || day0Token.isBlank()) {
            return;
        }

        try {
            // 1. Fetch all authorizations using Day-0 token
            AuthorizationListResponseDTO authListResponse = RestAssured.given()
                    .header("Authorization", "Bearer " + day0Token)
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .when()
                    .post("/authorizations/list")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(AuthorizationListResponseDTO.class);

            List<AuthorizationDTO> allAuths = flattenAuthorizations(authListResponse.groups());
            List<UUID> superAdminAuthIds = allAuths.stream().map(AuthorizationDTO::id).toList();
            List<UUID> userAuthIds = allAuths.stream().filter(a -> a.key().startsWith("USER_")).map(AuthorizationDTO::id).toList();
            List<UUID> roleAuthIds = allAuths.stream().filter(a -> a.key().startsWith("ROLE_") || a.key().equals("AUTHORIZATION_LIST")).map(AuthorizationDTO::id).toList();
            List<UUID> configAuthIds = allAuths.stream().filter(a -> a.key().startsWith("SYSTEM_CONFIG_") || a.key().startsWith("ERROR_CONFIG_")).map(AuthorizationDTO::id).toList();
            List<UUID> alertsAuthIds = allAuths.stream().filter(a -> a.key().startsWith("ALERT_")).map(AuthorizationDTO::id).toList();
            List<UUID> auditAuthIds = allAuths.stream().filter(a -> a.key().startsWith("AUDIT_")).map(AuthorizationDTO::id).toList();

            // 2. Create Roles
            UUID superAdminRoleId = createRoleIfNotExists(day0Token, "ROLE_SUPER_ADMIN", "Super Administrator Role", superAdminAuthIds);
            UUID userAdminRoleId = createRoleIfNotExists(day0Token, "ROLE_USER_ADMIN", "User Officer Role", userAuthIds);
            UUID roleAdminRoleId = createRoleIfNotExists(day0Token, "ROLE_ROLE_ADMIN", "Role Officer Role", roleAuthIds);
            UUID configAdminRoleId = createRoleIfNotExists(day0Token, "ROLE_CONFIG_ADMIN", "Config Officer Role", configAuthIds);
            UUID alertsAdminRoleId = createRoleIfNotExists(day0Token, "ROLE_ALERTS_ADMIN", "Alerts Officer Role", alertsAuthIds);
            UUID auditViewerRoleId = createRoleIfNotExists(day0Token, "ROLE_AUDIT_VIEWER", "Audit Viewer Role", auditAuthIds);

            // 3. Create Persona Users
            createUserIfNotExists(day0Token, SUPER_ADMIN_USERNAME, SUPER_ADMIN_PASSWORD, List.of(superAdminRoleId), "+10000000001");
            createUserIfNotExists(day0Token, USER_OFFICER_USERNAME, USER_OFFICER_PASSWORD, List.of(userAdminRoleId), "+10000000002");
            createUserIfNotExists(day0Token, ROLE_OFFICER_USERNAME, ROLE_OFFICER_PASSWORD, List.of(roleAdminRoleId), "+10000000003");
            createUserIfNotExists(day0Token, CONFIG_OFFICER_USERNAME, CONFIG_OFFICER_PASSWORD, List.of(configAdminRoleId), "+10000000004");
            createUserIfNotExists(day0Token, ALERTS_OFFICER_USERNAME, ALERTS_OFFICER_PASSWORD, List.of(alertsAdminRoleId), "+10000000005");
            createUserIfNotExists(day0Token, AUDIT_OFFICER_USERNAME, AUDIT_OFFICER_PASSWORD, List.of(auditViewerRoleId), "+10000000006");

            BOOTSTRAPPED.set(true);
        } catch (Exception e) {
            // Ignore during intermediate test phases
        }
    }

    private static UUID createRoleIfNotExists(String token, String name, String description, List<UUID> authIds) {
        try {
            return UUID.fromString(
                    RestAssured.given()
                            .header("Authorization", "Bearer " + token)
                            .contentType(ContentType.JSON)
                            .body(new RoleCreateRequestDTO(name, description, authIds))
                            .when()
                            .post("/roles/create")
                            .then()
                            .extract()
                            .jsonPath()
                            .getString("id")
            );
        } catch (Exception e) {
            return UUID.randomUUID();
        }
    }

    private static void createUserIfNotExists(String token, String username, String password, List<UUID> roleIds, String phone) {
        try {
            RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(new UserCreateRequestDTO(username, password, roleIds, com.freelance.alerts.AlertChannel.WHATSAPP, phone))
                    .when()
                    .post("/users/create");
        } catch (Exception ignored) {}
    }

    public static List<AuthorizationDTO> flattenAuthorizations(List<AuthorizationGroupDTO> groups) {
        List<AuthorizationDTO> list = new ArrayList<>();
        if (groups == null) return list;
        for (AuthorizationGroupDTO group : groups) {
            if (group.authorizations() != null) {
                list.addAll(group.authorizations());
            }
            if (group.subGroups() != null) {
                list.addAll(flattenAuthorizations(group.subGroups()));
            }
        }
        return list;
    }

    public String loginAndGetToken(String username, String password) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new com.freelance.auth.dto.AuthLoginRequestDTO(username, password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }

    public String getSuperAdminToken() {
        return loginAndGetToken(SUPER_ADMIN_USERNAME, SUPER_ADMIN_PASSWORD);
    }

    public String getUserOfficerToken() {
        return loginAndGetToken(USER_OFFICER_USERNAME, USER_OFFICER_PASSWORD);
    }

    public String getRoleOfficerToken() {
        return loginAndGetToken(ROLE_OFFICER_USERNAME, ROLE_OFFICER_PASSWORD);
    }

    public String getConfigOfficerToken() {
        return loginAndGetToken(CONFIG_OFFICER_USERNAME, CONFIG_OFFICER_PASSWORD);
    }

    public String getAlertsOfficerToken() {
        return loginAndGetToken(ALERTS_OFFICER_USERNAME, ALERTS_OFFICER_PASSWORD);
    }

    public String getAuditOfficerToken() {
        return loginAndGetToken(AUDIT_OFFICER_USERNAME, AUDIT_OFFICER_PASSWORD);
    }

    @AfterAll
    public static void flushCoverage() {
        try {
            RestAssured.given()
                    .when()
                    .post("/testing/coverage/dump")
                    .then()
                    .statusCode(200);
        } catch (Exception ignored) {}
    }
}


