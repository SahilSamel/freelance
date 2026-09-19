package com.freelance.tests.authorization;

import com.freelance.authorization.dto.list.AuthorizationListRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@DisplayName("POST /authorizations/list Integration Tests")
class AuthorizationListIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("role_officer: lists all authorizations successfully (200 OK)")
    void listAuthorizations_roleOfficer_returns200() {
        String token = getRoleOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new AuthorizationListRequestDTO(null))
                .when()
                .post("/authorizations/list")
                .then()
                .statusCode(200)
                .body("groups", notNullValue())
                .body("groups.size()", greaterThan(0))
                .body("totalCount", greaterThan(0));
    }

    @Test
    @DisplayName("cross-boundary RBAC: user_officer cannot list authorizations (403 FL_4101)")
    void listAuthorizations_userOfficer_forbidden() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new AuthorizationListRequestDTO(null))
                .when()
                .post("/authorizations/list")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_4101"));
    }

    @Test
    @DisplayName("no Authorization header returns 400 or 401")
    void listAuthorizations_noToken_fails() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthorizationListRequestDTO(null))
                .when()
                .post("/authorizations/list")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(401)));
    }
}

