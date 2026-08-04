package com.freelance.tests.auth;

import com.freelance.auth.dto.AuthLoginRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for POST /auth/login
 * <p>
 * Expects a user with username="testuser" and password="Test@1234" to exist in the DB.
 * Create it manually once via your identity management endpoint before running.
 */
@DisplayName("POST /auth/login")
class LoginTest extends BaseIntegrationTest {

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("valid credentials → 200 with a non-blank token")
    void login_validCredentials_returns200WithToken() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("testuser", "Test@1234"))
                .when()
                .post("/auth/login")
                .then()
                .log().all()
                .statusCode(200)
                .body("token", notNullValue())
                .body("token", not(emptyString()));
    }

    // ── Wrong credentials ─────────────────────────────────────────────────────

    @Test
    @DisplayName("wrong password → 400 INVALID_CREDENTIALS constraint error")
    void login_wrongPassword_returns400() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("testuser", "wrongpassword"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("CONSTRAINT_ERROR"));
    }

    @Test
    @DisplayName("unknown username → 400 INVALID_CREDENTIALS constraint error")
    void login_unknownUsername_returns400() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("no_such_user_xyz", "Test@1234"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("CONSTRAINT_ERROR"));
    }

    // ── Validation errors (format) ────────────────────────────────────────────

    @Test
    @DisplayName("blank username → 400 VALIDATION_ERROR")
    void login_blankUsername_returns400ValidationError() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("", "Test@1234"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("VALIDATION_ERROR"))
                .body("message", hasItem(hasEntry("errorCode", "INVALID_USERNAME")));
    }

    @Test
    @DisplayName("blank password → 400 VALIDATION_ERROR")
    void login_blankPassword_returns400ValidationError() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("testuser", ""))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("VALIDATION_ERROR"))
                .body("message", hasItem(hasEntry("errorCode", "INVALID_PASSWORD")));
    }

    @Test
    @DisplayName("username too short → 400 VALIDATION_ERROR")
    void login_usernameTooShort_returns400ValidationError() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("ab", "Test@1234"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("VALIDATION_ERROR"))
                .body("message", hasItem(hasEntry("errorCode", "INVALID_USERNAME")));
    }

    @Test
    @DisplayName("null body → 400")
    void login_nullBody_returns400() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400);
    }
}
