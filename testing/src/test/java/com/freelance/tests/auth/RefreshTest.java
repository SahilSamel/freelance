package com.freelance.tests.auth;

import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Integration tests for POST /auth/refresh
 * <p>
 * These tests also act as a regression suite for the session key rotation fix:
 * after a refresh, the OLD token must be dead even though its JWT signature
 * is still cryptographically valid.
 */
@DisplayName("POST /auth/refresh")
class RefreshTest extends BaseIntegrationTest {

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("valid token → 200 with a new non-blank token")
    void refresh_validToken_returns200WithNewToken() {
        String token = loginAndGetToken("testuser", "Test@1234");

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
    @DisplayName("refreshed token differs from the original token")
    void refresh_newTokenDiffersFromOldToken() {
        String oldToken = loginAndGetToken("testuser", "Test@1234");

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

    // ── Regression: session key rotation ─────────────────────────────────────

    @Test
    @DisplayName("[REGRESSION] old token is rejected after refresh (session key was rotated)")
    void refresh_oldTokenRejectedAfterRefresh() {
        String oldToken = loginAndGetToken("testuser", "Test@1234");

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
                .post("/auth/refresh")   // any protected endpoint works; using refresh again
                .then()
                .statusCode(400);        // INVALID_SESSION from VerifySessionConstraint
    }

    @Test
    @DisplayName("[REGRESSION] new token works after refresh")
    void refresh_newTokenIsUsableAfterRefresh() {
        String oldToken = loginAndGetToken("testuser", "Test@1234");

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

    // ── Missing / invalid token ───────────────────────────────────────────────

    @Test
    @DisplayName("no Authorization header → 400")
    void refresh_noToken_returns400() {
        RestAssured
                .given()
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("malformed JWT → 400")
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
