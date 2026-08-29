package com.freelance.tests.authorization;

import com.freelance.authorization.dto.list.AuthorizationListRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@DisplayName("POST /authorizations/list")
class AuthorizationListIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("valid token with SUPER_ADMIN role returns 200 and list of authorizations")
    void listAuthorizations_superAdmin_returns200() {
        String token = loginAsSuperAdmin();

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
    @DisplayName("no Authorization header returns 400")
    void listAuthorizations_noToken_returns400() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new AuthorizationListRequestDTO(null))
                .when()
                .post("/authorizations/list")
                .then()
                .statusCode(400);
    }
}
