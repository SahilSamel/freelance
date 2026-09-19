package com.freelance.tests.role;

import com.freelance.authorization.dto.role.create.RoleCreateRequestDTO;
import com.freelance.authorization.dto.role.delete.RoleDeleteRequestDTO;
import com.freelance.authorization.dto.role.list.RoleListRequestDTO;
import com.freelance.authorization.dto.role.read.RoleReadRequestDTO;
import com.freelance.authorization.dto.role.update.RoleUpdateRequestDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

@DisplayName("Authorization Role CRUD & Security Integration Tests")
class RoleCrudIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("role_officer: full role lifecycle (create -> read -> list -> update -> delete)")
    void fullRoleLifecycle_success() {
        String token = getRoleOfficerToken();

        // 1. Create Role
        String roleName = "INTEGRATION_ROLE_" + System.currentTimeMillis();
        String roleIdStr = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleCreateRequestDTO(roleName, "Role for integration testing", Collections.emptyList()))
                .when()
                .post("/roles/create")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo(roleName))
                .body("isSystem", equalTo(false))
                .extract().path("id");

        UUID roleId = UUID.fromString(roleIdStr);

        // 2. Read Role
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleReadRequestDTO(roleId, null))
                .when()
                .post("/roles/read")
                .then()
                .statusCode(200)
                .body("roleDTO.id", equalTo(roleIdStr))
                .body("roleDTO.name", equalTo(roleName));

        // 3. List Roles
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleListRequestDTO(null))
                .when()
                .post("/roles/list")
                .then()
                .statusCode(200)
                .body("roles", notNullValue())
                .body("roles.size()", greaterThan(0));

        // 4. Update Role
        String updatedName = roleName + "_UPDATED";
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleUpdateRequestDTO(roleId, updatedName, "Updated desc", Collections.emptyList()))
                .when()
                .post("/roles/update")
                .then()
                .statusCode(200)
                .body("name", equalTo(updatedName))
                .body("description", equalTo("Updated desc"));

        // 5. Delete Custom Role
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleDeleteRequestDTO(roleId))
                .when()
                .post("/roles/delete")
                .then()
                .statusCode(200)
                .body("deleted", equalTo(true));
    }

    @Test
    @DisplayName("create role with duplicate name returns 409 FL_4102")
    void createRole_duplicateName_returns409() {
        String token = getRoleOfficerToken();
        String roleName = "DUP_ROLE_" + System.currentTimeMillis();

        // 1st create
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleCreateRequestDTO(roleName, "desc", Collections.emptyList()))
                .when()
                .post("/roles/create")
                .then()
                .statusCode(201);

        // 2nd create
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleCreateRequestDTO(roleName, "desc", Collections.emptyList()))
                .when()
                .post("/roles/create")
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("FL_4102"));
    }

    @Test
    @DisplayName("create role with blank name returns 400 validation error (FL_1001)")
    void createRole_blankName_returns400() {
        String token = getRoleOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleCreateRequestDTO("", "desc", Collections.emptyList()))
                .when()
                .post("/roles/create")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_1001"));
    }

    @Test
    @DisplayName("create role with non-existent authorization ID returns 400 (FL_4107)")
    void createRole_nonExistentAuthId_returns400() {
        String token = getRoleOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleCreateRequestDTO("ROLE_WITH_BAD_AUTH_" + System.currentTimeMillis(), "desc", List.of(UUID.randomUUID())))
                .when()
                .post("/roles/create")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("FL_4107"));
    }

    @Test
    @DisplayName("read non-existent role ID returns 404 (FL_4103)")
    void readRole_nonExistentId_returns404() {
        String token = getRoleOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleReadRequestDTO(UUID.randomUUID(), null))
                .when()
                .post("/roles/read")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("FL_4103"));
    }

    @Test
    @DisplayName("update non-existent role returns 404 (FL_4104)")
    void updateRole_nonExistentId_returns404() {
        String token = getRoleOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleUpdateRequestDTO(UUID.randomUUID(), "NEW_NAME", "desc", Collections.emptyList()))
                .when()
                .post("/roles/update")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("FL_4104"));
    }

    @Test
    @DisplayName("delete non-existent role returns 404 (FL_4105)")
    void deleteRole_nonExistentId_returns404() {
        String token = getRoleOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleDeleteRequestDTO(UUID.randomUUID()))
                .when()
                .post("/roles/delete")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("FL_4105"));
    }

    // ── Cross-boundary RBAC Security Tests ────────────────────────────────────

    @Test
    @DisplayName("cross-boundary RBAC: user_officer cannot create roles (403 FL_4101)")
    void rbac_userOfficerCannotCreateRole() {
        String token = getUserOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleCreateRequestDTO("UNAUTHORIZED_ROLE", "desc", Collections.emptyList()))
                .when()
                .post("/roles/create")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_4101"));
    }

    @Test
    @DisplayName("cross-boundary RBAC: audit_officer cannot delete roles (403 FL_4101)")
    void rbac_auditOfficerCannotDeleteRole() {
        String token = getAuditOfficerToken();

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleDeleteRequestDTO(UUID.randomUUID()))
                .when()
                .post("/roles/delete")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FL_4101"));
    }
}

