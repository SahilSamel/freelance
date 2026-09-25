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
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseIntegrationTest {

    protected static final String BASE_URL = "https://localhost:8080";

    public static final String CONTROL_TENANT = "freelance";
    public static final String PRIMARY_TENANT = "testtenant";
    public static final String SECONDARY_TENANT = "secondary_tenant";

    public static final String CONTROL_ADMIN_USERNAME = "controladmin";
    public static final String CONTROL_ADMIN_PASSWORD = "ControlAdmin@123";

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
                                .jackson2ObjectMapperFactory((cls, charset) -> new ObjectMapper().findAndRegisterModules())
                );

        RestAssured.requestSpecification = new io.restassured.builder.RequestSpecBuilder()
                .addHeader("X-Tenant-ID", PRIMARY_TENANT)
                .build();

        ensureSystemBootstrapped();
    }

    public static RequestSpecification givenTenant(String tenantId) {
        return RestAssured.given().header("X-Tenant-ID", tenantId);
    }

    public static RequestSpecification givenControlPlane() {
        return givenTenant(CONTROL_TENANT);
    }

    public static RequestSpecification givenPrimaryTenant() {
        return givenTenant(PRIMARY_TENANT);
    }

    public static RequestSpecification givenSecondaryTenant() {
        return givenTenant(SECONDARY_TENANT);
    }

    public static String getDay0VendorToken() {
        return getDay0VendorToken(PRIMARY_TENANT);
    }

    public static String getDay0VendorToken(String tenantId) {
        try {
            if (PRIMARY_TENANT.equalsIgnoreCase(tenantId) && com.freelance.initialization.Init.getDefaultTenantResult() != null) {
                return com.freelance.initialization.Init.getDefaultTenantResult().day0Token();
            }
        } catch (Exception ignored) {}

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/freelance_db", "postgres", "welcome1");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                "SELECT config_value FROM freelance.FL_SYSTEM_CONFIGURATION WHERE config_id = 'control.platform.vendor.privateKey'");
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String pem = rs.getString(1);
                String base64Key = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s+", "");
                byte[] keyBytes = java.util.Base64.getDecoder().decode(base64Key);
                java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
                java.security.PrivateKey privateKey = kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(keyBytes));
                java.time.Instant now = java.time.Instant.now();
                return io.jsonwebtoken.Jwts.builder()
                        .issuer(com.freelance.auth.token.TokenUtility.VENDOR_ISSUER)
                        .subject(com.freelance.system.SystemHardcoding.VENDOR_SUPPORT_USER_ID.asString())
                        .claim("sessionKey", com.freelance.system.SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString())
                        .claim("tenantId", tenantId)
                        .issuedAt(java.util.Date.from(now))
                        .expiration(java.util.Date.from(now.plus(1, java.time.temporal.ChronoUnit.HOURS)))
                        .signWith(privateKey)
                        .compact();
            }
        } catch (Exception e) {
            System.err.println("[BaseIntegrationTest] Failed to read vendor private key via JDBC: " + e.getMessage());
        }

        return null;
    }

    public static synchronized void ensureSystemBootstrapped() {
        if (BOOTSTRAPPED.get()) {
            return;
        }

        // Check if super_admin can already log in
        try {
            int status = RestAssured.given()
                    .header("X-Tenant-ID", PRIMARY_TENANT)
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

        String day0Token = getDay0VendorToken(PRIMARY_TENANT);
        if (day0Token == null || day0Token.isBlank()) {
            return;
        }

        try {
            // 1. Fetch all authorizations using Day-0 token
            AuthorizationListResponseDTO authListResponse = RestAssured.given()
                    .header("X-Tenant-ID", PRIMARY_TENANT)
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
            UUID superAdminRoleId = createRoleIfNotExists(day0Token, PRIMARY_TENANT, "ROLE_SUPER_ADMIN", "Super Administrator Role", superAdminAuthIds);
            UUID userAdminRoleId = createRoleIfNotExists(day0Token, PRIMARY_TENANT, "ROLE_USER_ADMIN", "User Officer Role", userAuthIds);
            UUID roleAdminRoleId = createRoleIfNotExists(day0Token, PRIMARY_TENANT, "ROLE_ROLE_ADMIN", "Role Officer Role", roleAuthIds);
            UUID configAdminRoleId = createRoleIfNotExists(day0Token, PRIMARY_TENANT, "ROLE_CONFIG_ADMIN", "Config Officer Role", configAuthIds);
            UUID alertsAdminRoleId = createRoleIfNotExists(day0Token, PRIMARY_TENANT, "ROLE_ALERTS_ADMIN", "Alerts Officer Role", alertsAuthIds);
            UUID auditViewerRoleId = createRoleIfNotExists(day0Token, PRIMARY_TENANT, "ROLE_AUDIT_VIEWER", "Audit Viewer Role", auditAuthIds);

            // 3. Create Persona Users
            createUserIfNotExists(day0Token, PRIMARY_TENANT, SUPER_ADMIN_USERNAME, SUPER_ADMIN_PASSWORD, List.of(superAdminRoleId), "+10000000001");
            createUserIfNotExists(day0Token, PRIMARY_TENANT, USER_OFFICER_USERNAME, USER_OFFICER_PASSWORD, List.of(userAdminRoleId), "+10000000002");
            createUserIfNotExists(day0Token, PRIMARY_TENANT, ROLE_OFFICER_USERNAME, ROLE_OFFICER_PASSWORD, List.of(roleAdminRoleId), "+10000000003");
            createUserIfNotExists(day0Token, PRIMARY_TENANT, CONFIG_OFFICER_USERNAME, CONFIG_OFFICER_PASSWORD, List.of(configAdminRoleId), "+10000000004");
            createUserIfNotExists(day0Token, PRIMARY_TENANT, ALERTS_OFFICER_USERNAME, ALERTS_OFFICER_PASSWORD, List.of(alertsAdminRoleId), "+10000000005");
            createUserIfNotExists(day0Token, PRIMARY_TENANT, AUDIT_OFFICER_USERNAME, AUDIT_OFFICER_PASSWORD, List.of(auditViewerRoleId), "+10000000006");

            BOOTSTRAPPED.set(true);
        } catch (Exception e) {
            System.err.println("[BaseIntegrationTest] Failed to bootstrap test personas: " + e.getMessage());
        }
    }

    public static UUID createRoleIfNotExists(String token, String tenantId, String name, String description, List<UUID> authIds) {
        try {
            return UUID.fromString(
                    RestAssured.given()
                            .header("X-Tenant-ID", tenantId)
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

    public static void createUserIfNotExists(String token, String tenantId, String username, String password, List<UUID> roleIds, String phone) {
        try {
            RestAssured.given()
                    .header("X-Tenant-ID", tenantId)
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

    public String loginAndGetToken(String tenantId, String username, String password) {
        return RestAssured.given()
                .header("X-Tenant-ID", tenantId)
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

    public String loginAndGetToken(String username, String password) {
        return loginAndGetToken(PRIMARY_TENANT, username, password);
    }

    public String getControlAdminToken() {
        return loginAndGetToken(CONTROL_TENANT, CONTROL_ADMIN_USERNAME, CONTROL_ADMIN_PASSWORD);
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
