package com.freelance.tests.auth;

import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Order(42)
@DisplayName("Stage 04.2: Customer Session Key Rotation & Token Refresh")
@TestMethodOrder(OrderAnnotation.class)
class RefreshTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("1. Valid token → 200 with a new non-blank token")
    void refresh_validToken_returns200WithNewToken() {
        String token = getSuperAdminToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("token", not(emptyString()));
    }

    @Test
    @Order(2)
    @DisplayName("2. Refreshed token differs from the original token")
    void refresh_newTokenDiffersFromOldToken() {
        String oldToken = getSuperAdminToken();

        String newToken = RestAssured
                .given()
                .header("Authorization", "Bearer " + oldToken)
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");

        assertNotEquals(oldToken, newToken, "Refreshed token must be different from the original");
    }

    @Test
    @Order(3)
    @DisplayName("3. [REGRESSION] Old token is rejected after refresh (session key was rotated)")
    void refresh_oldTokenRejectedAfterRefresh() {
        String oldToken = getSuperAdminToken();

        // Perform refresh — server rotates the session key
        RestAssured
                .given()
                .header("Authorization", "Bearer " + oldToken)
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(200);

        // Old token still has a valid JWT signature BUT its session key no longer
        // exists in the cache — so verify() must reject it.
        RestAssured
                .given()
                .header("Authorization", "Bearer " + oldToken)
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    @DisplayName("4. [REGRESSION] New token is usable after refresh")
    void refresh_newTokenIsUsableAfterRefresh() {
        String oldToken = getSuperAdminToken();

        String newToken = RestAssured
                .given()
                .header("Authorization", "Bearer " + oldToken)
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");

        // New token must work on a subsequent protected call
        RestAssured
                .given()
                .header("Authorization", "Bearer " + newToken)
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(5)
    @DisplayName("5. No Authorization header → 400")
    void refresh_noToken_returns400() {
        RestAssured
                .given()
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("6. Malformed JWT → 400")
    void refresh_malformedToken_returns400() {
        RestAssured
                .given()
                .header("Authorization", "Bearer not.a.real.jwt")
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(400);
    }
}
