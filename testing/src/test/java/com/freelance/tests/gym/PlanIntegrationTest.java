package com.freelance.tests.gym;

import com.freelance.gym.plan.dto.PlanCreateRequestDTO;
import com.freelance.gym.plan.dto.PlanDeleteRequestDTO;
import com.freelance.gym.plan.dto.PlanListResponseDTO;
import com.freelance.gym.plan.dto.PlanResponseDTO;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Order(70)
@DisplayName("Stage 07.1: Gym Plan API Integration Tests")
class PlanIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Create plan, list plans, and soft delete plan")
    void testPlanLifecycle_success() {
        String token = getSuperAdminToken();

        String planName = "Floor Access " + System.currentTimeMillis();

        // 1. Create Plan
        PlanCreateRequestDTO createRequest = new PlanCreateRequestDTO(
                planName,
                "Full gym floor equipment access",
                BigDecimal.valueOf(1500.00),
                30
        );

        PlanResponseDTO createResponse = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createRequest)
                .when()
                .post("/plan/new")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo(planName))
                .body("defaultDuration", equalTo(30))
                .body("isActive", equalTo(true))
                .extract()
                .as(PlanResponseDTO.class);

        assertNotNull(createResponse.id());
        assertEquals(30, createResponse.defaultDuration());
        UUID planId = createResponse.id();

        // 2. List Plans
        PlanListResponseDTO listResponse = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get("/plan/list")
                .then()
                .statusCode(200)
                .body("plans", notNullValue())
                .extract()
                .as(PlanListResponseDTO.class);

        assertTrue(listResponse.plans().stream().anyMatch(p -> p.id().equals(planId)));

        // 3. Soft Delete Plan
        PlanResponseDTO deletedResponse = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new PlanDeleteRequestDTO(planId))
                .when()
                .post("/plan/delete")
                .then()
                .statusCode(200)
                .body("id", equalTo(planId.toString()))
                .body("isActive", equalTo(false))
                .extract()
                .as(PlanResponseDTO.class);

        assertFalse(deletedResponse.isActive());
    }

    @Test
    @DisplayName("Create plan with duplicate name returns constraint error")
    void testCreatePlan_duplicateNameFails() {
        String token = getSuperAdminToken();
        String uniqueName = "Duplicate Test Plan " + System.currentTimeMillis();

        PlanCreateRequestDTO request = new PlanCreateRequestDTO(
                uniqueName,
                "Testing duplicates",
                BigDecimal.valueOf(500.00),
                30
        );

        // First creation succeeds
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/plan/new")
                .then()
                .statusCode(200);

        // Second creation with same name fails with constraint violation
        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/plan/new")
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("Create plan with empty name fails validation")
    void testCreatePlan_blankNameFailsValidation() {
        String token = getSuperAdminToken();

        PlanCreateRequestDTO request = new PlanCreateRequestDTO(
                "",
                "No name plan",
                BigDecimal.valueOf(500.00),
                30
        );

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/plan/new")
                .then()
                .statusCode(400);
    }
}
