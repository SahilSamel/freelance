package com.freelance.tests.auth;

import com.freelance.auth.dto.AuthLoginRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.*;

@Order(41)
@DisplayName("Stage 04.1: Customer Authentication & Credential Validation")
@TestMethodOrder(OrderAnnotation.class)
class LoginTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("1. Valid superadmin credentials → 200 with a non-blank token")
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

    @Test
    @Order(2)
    @DisplayName("2. Wrong password → 400 INVALID_CREDENTIALS (FL_2101)")
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
    @Order(3)
    @DisplayName("3. Unknown username → 400 INVALID_CREDENTIALS (FL_2101)")
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

    @Test
    @Order(4)
    @DisplayName("4. Blank username → 400 VALIDATION_ERROR (FL_1001) with item FL_2001")
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
    @Order(5)
    @DisplayName("5. Blank password → 400 VALIDATION_ERROR (FL_1001) with item FL_2002")
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
    @Order(6)
    @DisplayName("6. Username too short → 400 VALIDATION_ERROR (FL_1001) with item FL_2001")
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
    @Order(7)
    @DisplayName("7. Null body → 400")
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
