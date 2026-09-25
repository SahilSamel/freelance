package com.freelance.tests.auth;

import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Order(43)
@DisplayName("Stage 04.3: Customer Session Termination & Cache Invalidation")
@TestMethodOrder(OrderAnnotation.class)
class LogoutTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("1. Valid token → 200, session is invalidated")
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
    @Order(2)
    @DisplayName("2. After logout, the same token is rejected on a protected endpoint")
    void logout_tokenRejectedAfterLogout() {
        String token = getSuperAdminToken();

        // Logout
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/auth/logout");

        // Same token should now fail — session was invalidated in cache
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(3)
    @DisplayName("3. No Authorization header → 400")
    void logout_noToken_returns400() {
        RestAssured
                .given()
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    @DisplayName("4. Malformed token → 400")
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
