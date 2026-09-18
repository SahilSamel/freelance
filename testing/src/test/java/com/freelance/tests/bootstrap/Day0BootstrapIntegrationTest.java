package com.freelance.tests.bootstrap;

import com.freelance.tests.BaseIntegrationTest;
import com.freelance.tests.support.VendorSupportTestHelper;
import com.freelance.user.create.UserCreateRequestDTO;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.security.KeyPair;
import java.time.Duration;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

/**
 * Integration test verifying Day-0 onboarding and vendor support access lifecycle:
 * 1. Admin/Installer uses Day-0 Vendor Support token to create initial root admin user via POST /users/create.
 * 2. Newly created user is readable.
 * 3. Day-0 window is revoked via POST /support/revoke-access.
 * 4. Subsequent requests using the vendor support token fail with SUPPORT_ACCESS_DISABLED (FL_2105).
 */
@DisplayName("Day-0 Bootstrap Onboarding Integration Test")
@TestMethodOrder(OrderAnnotation.class)
class Day0BootstrapIntegrationTest extends BaseIntegrationTest {

    private static final KeyPair keyPair = VendorSupportTestHelper.generateRsaKeyPair();
    private static final String UNIQUE_SUFFIX = UUID.randomUUID().toString().substring(0, 8);
    private static final String ROOT_ADMIN_USERNAME = "root_admin_" + UNIQUE_SUFFIX;

    private static String getDay0VendorToken() {
        return VendorSupportTestHelper.generateSupportToken(
                keyPair.getPrivate(),
                Duration.ofHours(24)
        );
    }

    @Test
    @Order(1)
    @DisplayName("1. Day-0 Vendor Support token allows initial user creation (POST /users/create)")
    void step1_day0TokenCreatesRootAdmin() {
        String token = getDay0VendorToken();

        // Ensure support access is active for this test session
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .when()
                .post("/support/grant-access")
                .then()
                .statusCode(200);

        UserCreateRequestDTO createRequest = new UserCreateRequestDTO(
                ROOT_ADMIN_USERNAME,
                "RootSecurePass123!"
        );

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createRequest)
                .when()
                .post("/users/create")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("username", equalTo(ROOT_ADMIN_USERNAME));
    }

    @Test
    @Order(2)
    @DisplayName("2. Revoking support access terminates Day-0 session immediately")
    void step2_revokeSupportAccess() {
        String token = getDay0VendorToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .post("/support/revoke-access")
                .then()
                .statusCode(200)
                .body("revoked", equalTo(true));
    }

    @Test
    @Order(3)
    @DisplayName("3. Subsequent requests with Day-0 token fail with 400 SUPPORT_ACCESS_DISABLED (FL_2105)")
    void step3_subsequentDay0TokenCallFails() {
        String token = getDay0VendorToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/audit/list")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_2105"));
    }
}
