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
 */
@DisplayName("POST /auth/login")
class LoginTest extends BaseIntegrationTest {

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("valid superadmin credentials → 200 with a non-blank token")
    void login_validCredentials_returns200WithToken() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO(SUPER_ADMIN_USERNAME, SUPER_ADMIN_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("token", not(emptyString()));
    }

    // ── Wrong credentials ─────────────────────────────────────────────────────

    @Test
    @DisplayName("wrong password → 400 INVALID_CREDENTIALS (FL_2101)")
    void login_wrongPassword_returns400() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO(SUPER_ADMIN_USERNAME, "WrongPassword123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_2101"));
    }

    @Test
    @DisplayName("unknown username → 400 INVALID_CREDENTIALS (FL_2101)")
    void login_unknownUsername_returns400() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("no_such_user_xyz", SUPER_ADMIN_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_2101"));
    }

    // ── Validation errors (format) ────────────────────────────────────────────

    @Test
    @DisplayName("blank username → 400 VALIDATION_ERROR (FL_1001) with item FL_2001")
    void login_blankUsername_returns400ValidationError() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("", SUPER_ADMIN_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"))
                .body("message", hasItem(hasEntry("errorCode", "FL_2001")));
    }

    @Test
    @DisplayName("blank password → 400 VALIDATION_ERROR (FL_1001) with item FL_2002")
    void login_blankPassword_returns400ValidationError() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO(SUPER_ADMIN_USERNAME, ""))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"))
                .body("message", hasItem(hasEntry("errorCode", "FL_2002")));
    }

    @Test
    @DisplayName("username too short → 400 VALIDATION_ERROR (FL_1001) with item FL_2001")
    void login_usernameTooShort_returns400ValidationError() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthLoginRequestDTO("ab", SUPER_ADMIN_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"))
                .body("message", hasItem(hasEntry("errorCode", "FL_2001")));
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

