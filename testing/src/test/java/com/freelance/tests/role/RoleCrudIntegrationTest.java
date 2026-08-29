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
import java.util.UUID;

import static org.hamcrest.Matchers.*;

@DisplayName("Role CRUD Integration Tests")
class RoleCrudIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Complete Role lifecycle: create, read, list, update, reject system role mutation, delete")
    void testRoleLifecycle() {
        String token = loginAsSuperAdmin();

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

        // 5. Attempt to update a system role (SUPER_ADMIN)
        // Find SUPER_ADMIN role ID from list
        String superAdminIdStr = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new RoleListRequestDTO(true))
                .when()
                .post("/roles/list")
                .then()
                .statusCode(200)
                .extract().path("roles[0].id");

        if (superAdminIdStr != null) {
            UUID superAdminId = UUID.fromString(superAdminIdStr);

            RestAssured
                    .given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(new RoleUpdateRequestDTO(superAdminId, "HACKED_SUPER_ADMIN", "desc", null))
                    .when()
                    .post("/roles/update")
                    .then()
                    .statusCode(400)
                    .body("errorCode", equalTo("FL_4106"));

            RestAssured
                    .given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(new RoleDeleteRequestDTO(superAdminId))
                    .when()
                    .post("/roles/delete")
                    .then()
                    .statusCode(400)
                    .body("errorCode", equalTo("FL_4106"));
        }

        // 6. Delete Custom Role
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
}
