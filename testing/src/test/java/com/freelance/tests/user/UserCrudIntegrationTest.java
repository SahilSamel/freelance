package com.freelance.tests.user;

import com.freelance.tests.BaseIntegrationTest;
import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.delete.UserDeleteRequestDTO;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.update.UserUpdateRequestDTO;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;

@DisplayName("Identity Management User CRUD Integration Tests")
class UserCrudIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("full user lifecycle: create -> read -> update -> delete")
    void fullUserLifecycle_success() {
        String token = loginAsSuperAdmin();
        String uniqueUsername = "crud_user_" + System.currentTimeMillis();

        // 1. Create User
        String userIdStr = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(uniqueUsername, "SecurePass123"))
                .when()
                .post("/users/create")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("username", equalTo(uniqueUsername))
                .extract()
                .jsonPath()
                .getString("id");

        UUID userId = UUID.fromString(userIdStr);

        // 2. Read User
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserReadRequestDTO(userId, null))
                .when()
                .post("/users/read")
                .then()
                .statusCode(200)
                .body("userDTO.id", equalTo(userIdStr))
                .body("userDTO.username", equalTo(uniqueUsername));

        // 3. Update User
        String updatedUsername = uniqueUsername + "_updated";
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserUpdateRequestDTO(userId, updatedUsername, "UpdatedPassword456"))
                .when()
                .post("/users/update")
                .then()
                .statusCode(200)
                .body("id", equalTo(userIdStr))
                .body("username", equalTo(updatedUsername));

        // 4. Delete User
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserDeleteRequestDTO(userId))
                .when()
                .post("/users/delete")
                .then()
                .statusCode(200)
                .body("id", equalTo(userIdStr))
                .body("deleted", equalTo(true));

        // 5. Read after delete -> 404 USER_NOT_FOUND
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserReadRequestDTO(userId, null))
                .when()
                .post("/users/read")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("create user with blank username returns 400 validation error (FL_1001)")
    void createUser_blankUsername_returns400() {
        String token = loginAsSuperAdmin();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO("", "Pass1234"))
                .when()
                .post("/users/create")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"))
                .body("message", hasItem(hasEntry("errorCode", "FL_3002")));
    }

    @Test
    @DisplayName("create user with duplicate username returns 409 USER_ALREADY_EXISTS (FL_3101)")
    void createUser_duplicateUsername_returns409() {
        String token = loginAsSuperAdmin();

        String duplicateUsername = "dup_user_" + System.currentTimeMillis();
        // First create
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(duplicateUsername, "Pass1234"))
                .when()
                .post("/users/create")
                .then()
                .statusCode(201);

        // Attempt creating again with same username
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(duplicateUsername, "DifferentPass123"))
                .when()
                .post("/users/create")
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("FL_3101"));
    }
}
