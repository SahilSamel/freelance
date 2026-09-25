package com.freelance.tests.user;

import com.freelance.alerts.AlertChannel;
import com.freelance.tests.BaseIntegrationTest;
import com.freelance.user.contact.UserContactRequestDTO;
import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.delete.UserDeleteRequestDTO;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.update.UserUpdateRequestDTO;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

@Order(45)
@DisplayName("Stage 04.5: Identity Management User CRUD & Security Integration Tests")
class UserCrudIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("user_officer: full user lifecycle (create -> read by ID -> read by username -> update -> delete)")
    void fullUserLifecycle_success() {
        String token = getUserOfficerToken();
        String uniqueUsername = "test_user_" + System.currentTimeMillis();
        String uniquePhone = "+1555" + (System.currentTimeMillis() % 10000000);

        // 1. Create User with mandatory contact info
        String userIdStr = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(uniqueUsername, "SecurePass123!", AlertChannel.WHATSAPP, uniquePhone))
                .when()
                .post("/users/create")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("username", equalTo(uniqueUsername))
                .body("contacts", hasSize(1))
                .body("contacts[0].contactValue", equalTo(uniquePhone))
                .body("contacts[0].channel", equalTo("WHATSAPP"))
                .extract()
                .jsonPath()
                .getString("id");

        UUID userId = UUID.fromString(userIdStr);

        // 2. Read User by ID
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

        // 3. Read User by Username
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserReadRequestDTO(null, uniqueUsername))
                .when()
                .post("/users/read")
                .then()
                .statusCode(200)
                .body("userDTO.id", equalTo(userIdStr))
                .body("userDTO.username", equalTo(uniqueUsername));

        // 4. Update User (including contacts)
        String updatedUsername = uniqueUsername + "_u";
        String updatedWhatsApp = "+199" + String.format("%08d", System.currentTimeMillis() % 100000000);
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserUpdateRequestDTO(userId, updatedUsername, "UpdatedPassword456!", AlertChannel.WHATSAPP, updatedWhatsApp))
                .when()
                .post("/users/update")
                .then()
                .statusCode(200)
                .body("id", equalTo(userIdStr))
                .body("username", equalTo(updatedUsername))
                .body("contacts", hasSize(1))
                .body("contacts[0].channel", equalTo("WHATSAPP"))
                .body("contacts[0].contactValue", equalTo(updatedWhatsApp));

        // 5. Delete User (cascades to FL_USER_CONTACT and FL_USER_ROLE)
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

        // 6. Read after delete -> 404 USER_NOT_FOUND (FL_3102)
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserReadRequestDTO(userId, null))
                .when()
                .post("/users/read")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("FL_3102"));
    }

    @Test
    @DisplayName("create user with blank username returns 400 validation error (FL_1001)")
    void createUser_blankUsername_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO("", "Pass1234!", AlertChannel.WHATSAPP, "+15551112233"))
                .when()
                .post("/users/create")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("create user with short username (<3 chars) returns 400 validation error")
    void createUser_shortUsername_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO("ab", "Pass1234!", AlertChannel.WHATSAPP, "+15551112234"))
                .when()
                .post("/users/create")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("create user with short password (<8 chars) returns 400 validation error")
    void createUser_shortPassword_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO("valid_user_" + System.currentTimeMillis(), "short", AlertChannel.WHATSAPP, "+15551112235"))
                .when()
                .post("/users/create")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("create user without contacts returns 400 validation error (FL_1001)")
    void createUser_withoutContacts_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO("user_nocontact_" + System.currentTimeMillis(), "ValidPass123!"))
                .when()
                .post("/users/create")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("create user with blank contact value returns 400 validation error (FL_1001)")
    void createUser_blankContactValue_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(
                        "user_blankcontact_" + System.currentTimeMillis(),
                        "ValidPass123!",
                        List.of(),
                        List.of(new UserContactRequestDTO(AlertChannel.WHATSAPP, "   "))
                ))
                .when()
                .post("/users/create")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("create user with duplicate channel returns 400 validation error (FL_1001)")
    void createUser_duplicateChannel_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(
                        "user_dupchan_" + System.currentTimeMillis(),
                        "ValidPass123!",
                        List.of(),
                        List.of(
                                new UserContactRequestDTO(AlertChannel.WHATSAPP, "+15553334455"),
                                new UserContactRequestDTO(AlertChannel.WHATSAPP, "+15553334466")
                        )
                ))
                .when()
                .post("/users/create")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("create user with duplicate username returns 409 USER_ALREADY_EXISTS (FL_3101)")
    void createUser_duplicateUsername_returns409() {
        String token = getUserOfficerToken();
        long ts = System.currentTimeMillis() % 10000000;
        String dupUsername = "dup_user_" + ts;
        String dupPhone1 = "+155" + String.format("%07d", ts);
        String dupPhone2 = "+156" + String.format("%07d", ts);

        // 1st create
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(dupUsername, "Pass1234!", AlertChannel.WHATSAPP, dupPhone1))
                .when()
                .post("/users/create")
                .then()
                .statusCode(201);

        // 2nd create with duplicate username
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO(dupUsername, "AnotherPass123!", AlertChannel.WHATSAPP, dupPhone2))
                .when()
                .post("/users/create")
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("FL_3101"));
    }

    @Test
    @DisplayName("create user with duplicate contact across users returns 409 CONTACT_ALREADY_EXISTS (FL_3106)")
    void createUser_duplicateContact_returns409() {
        String token = getUserOfficerToken();
        String phone = "+1555999" + (System.currentTimeMillis() % 10000);

        // 1st create
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO("user_contact_a_" + System.currentTimeMillis(), "Pass1234!", AlertChannel.WHATSAPP, phone))
                .when()
                .post("/users/create")
                .then()
                .statusCode(201);

        // 2nd create with duplicate contact value for same channel
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO("user_contact_b_" + System.currentTimeMillis(), "AnotherPass123!", AlertChannel.WHATSAPP, phone))
                .when()
                .post("/users/create")
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("FL_3106"));
    }

    @Test
    @DisplayName("read non-existent user by ID returns 404 USER_NOT_FOUND (FL_3102)")
    void readUser_nonExistentId_returns404() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserReadRequestDTO(UUID.randomUUID(), null))
                .when()
                .post("/users/read")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("FL_3102"));
    }

    @Test
    @DisplayName("read non-existent user by username returns 404 USER_NOT_FOUND (FL_3102)")
    void readUser_nonExistentUsername_returns404() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserReadRequestDTO(null, "non_existent_username_xyz"))
                .when()
                .post("/users/read")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("FL_3102"));
    }

    @Test
    @DisplayName("read user with blank request returns 400 validation error (FL_1001)")
    void readUser_blankRequest_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/users/read")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("update non-existent user returns 404 USER_TO_UPDATE_NOT_FOUND (FL_3104)")
    void updateUser_nonExistentId_returns404() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserUpdateRequestDTO(UUID.randomUUID(), "some_username", "NewPassword123!"))
                .when()
                .post("/users/update")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("FL_3104"));
    }

    @Test
    @DisplayName("update user with null ID returns 400 validation error (FL_1001)")
    void updateUser_nullId_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserUpdateRequestDTO(null, "some_username", "NewPassword123!"))
                .when()
                .post("/users/update")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("update user with blank contact value returns 400 validation error (FL_1001)")
    void updateUser_blankContactValue_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserUpdateRequestDTO(UUID.randomUUID(), "some_username", "NewPassword123!", AlertChannel.WHATSAPP, "   "))
                .when()
                .post("/users/update")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("update user with duplicate channels returns 400 validation error (FL_1001)")
    void updateUser_duplicateChannels_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserUpdateRequestDTO(
                        UUID.randomUUID(),
                        "some_username",
                        "NewPassword123!",
                        List.of(
                                new com.freelance.user.contact.UserContactRequestDTO(AlertChannel.WHATSAPP, "+15551111111"),
                                new com.freelance.user.contact.UserContactRequestDTO(AlertChannel.WHATSAPP, "+15552222222")
                        )
                ))
                .when()
                .post("/users/update")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("delete non-existent user returns 404 USER_TO_DELETE_NOT_FOUND (FL_3105)")
    void deleteUser_nonExistentId_returns404() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserDeleteRequestDTO(UUID.randomUUID()))
                .when()
                .post("/users/delete")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("FL_3105"));
    }

    @Test
    @DisplayName("delete user with null ID returns 400 validation error (FL_1001)")
    void deleteUser_nullId_returns400() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/users/delete")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    // ── Cross-boundary RBAC Security Tests ────────────────────────────────────

    @Test
    @DisplayName("cross-boundary RBAC: config_officer cannot create users (403 FL_4101)")
    void rbac_configOfficerCannotCreateUser() {
        String token = getConfigOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserCreateRequestDTO("unauthorized_user", "Password123!", AlertChannel.WHATSAPP, "+15550009988"))
                .when()
                .post("/users/create")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_4101"));
    }

    @Test
    @DisplayName("cross-boundary RBAC: alerts_officer cannot delete users (403 FL_4101)")
    void rbac_alertsOfficerCannotDeleteUser() {
        String token = getAlertsOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserDeleteRequestDTO(UUID.randomUUID()))
                .when()
                .post("/users/delete")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_4101"));
    }

    @Test
    @DisplayName("unauthenticated caller cannot access user endpoints (400 or 401)")
    void security_unauthenticatedCallFails() {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new UserReadRequestDTO(UUID.randomUUID(), null))
                .when()
                .post("/users/read")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(401)));
    }
}
