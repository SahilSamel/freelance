package com.freelance.tests.auth;

import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for POST /auth/logout
 */
@DisplayName("POST /auth/logout")
class LogoutTest extends BaseIntegrationTest {

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("valid token → 200, session is invalidated")
    void logout_validToken_returns200() {
        String token = getSuperAdminToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("after logout, the same token is rejected on a protected endpoint")
    void logout_tokenRejectedAfterLogout() {
        String token = getSuperAdminToken();

        // Logout
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/auth/logout");

        // Same token should now fail — session was invalidated
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(400);  // INVALID_SESSION from VerifySessionConstraint
    }

    // ── Missing / invalid token ───────────────────────────────────────────────

    @Test
    @DisplayName("no Authorization header → 400")
    void logout_noToken_returns400() {
        RestAssured
                .given()
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("malformed token → 400")
    void logout_malformedToken_returns400() {
        RestAssured
                .given()
                .header("Authorization", "Bearer this.is.not.a.valid.jwt")
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(400);
    }
}

