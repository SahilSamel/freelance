package com.freelance.tests.gym;

import com.freelance.gym.enums.PaymentMethod;
import com.freelance.gym.member.dto.*;
import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.gym.plan.dto.PlanCreateRequestDTO;
import com.freelance.gym.plan.dto.PlanResponseDTO;
import com.freelance.identityManagement.PhoneNumber;
import com.freelance.tests.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Order(71)
@DisplayName("Stage 07.2: Gym Member API Integration Tests")
class MemberIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Create prospect member with contact details only (no plans, no payments)")
    void testCreateProspectMember_success() {
        String token = getSuperAdminToken();
        String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
        String phoneNum = "98" + suffix.substring(Math.max(0, suffix.length() - 8));
        while (phoneNum.length() < 10) {
            phoneNum += "0";
        }

        MemberCreateRequestDTO request = new MemberCreateRequestDTO(
                "ProspectFirst",
                "ProspectLast",
                new PhoneNumber("91", phoneNum),
                List.of(),
                List.of()
        );

        MemberCreateResponseDTO response = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/members/new")
                .then()
                .statusCode(200)
                .body("userId", notNullValue())
                .body("username", notNullValue())
                .body("generatedPassword", notNullValue())
                .body("userTypes", hasItem("MEMBER"))
                .body("plans", empty())
                .body("payments", empty())
                .extract()
                .as(MemberCreateResponseDTO.class);

        assertNotNull(response.userId());
        assertTrue(response.plans().isEmpty());
        assertTrue(response.payments().isEmpty());
    }

    @Test
    @DisplayName("Full Active Member Flow: Onboard with installments -> List -> Renew (strictly additive) -> Update (delete-insert)")
    void testActiveMemberFullLifecycle_success() {
        String token = getSuperAdminToken();
        String suffix = String.valueOf(System.currentTimeMillis() % 10000000);
        String phoneNum = "99" + suffix.substring(Math.max(0, suffix.length() - 8));
        while (phoneNum.length() < 10) {
            phoneNum += "0";
        }

        // 1. Create a Base Plan
        PlanCreateRequestDTO planReq = new PlanCreateRequestDTO(
                "Gold Gym Membership " + suffix,
                "Full access",
                BigDecimal.valueOf(2000.00),
                30
        );
        PlanResponseDTO createdPlan = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(planReq)
                .when()
                .post("/plan/new")
                .then()
                .statusCode(200)
                .extract()
                .as(PlanResponseDTO.class);

        UUID planId = createdPlan.id();

        // 2. Onboard Member with 1 Month Plan and 2 Installments (1 PAID with txnId, 1 PENDING with null txnId)
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        MemberPlanAssignmentDTO planAssignment = new MemberPlanAssignmentDTO(
                planId,
                startDate,
                endDate
        );

        PaymentScheduleDTO inst1 = new PaymentScheduleDTO(
                BigDecimal.valueOf(1000.00),
                BigDecimal.valueOf(1000.00),
                startDate,
                startDate,
                PaymentMethod.UPI,
                "TXN_UPFRONT_001",
                "First installment (Paid upfront)"
        );

        PaymentScheduleDTO inst2 = new PaymentScheduleDTO(
                BigDecimal.valueOf(1000.00),
                BigDecimal.ZERO,
                startDate.plusDays(15),
                null,
                PaymentMethod.CASH,
                null,
                "Second installment (Pending)"
        );

        MemberCreateRequestDTO createReq = new MemberCreateRequestDTO(
                "Arnold",
                "Schwarzenegger",
                new PhoneNumber("91", phoneNum),
                List.of(planAssignment),
                List.of(inst1, inst2)
        );

        MemberCreateResponseDTO createdMember = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createReq)
                .when()
                .post("/members/new")
                .then()
                .statusCode(200)
                .body("userId", notNullValue())
                .body("plans", hasSize(1))
                .body("payments", hasSize(2))
                .extract()
                .as(MemberCreateResponseDTO.class);

        UUID memberUserId = createdMember.userId();
        assertNotNull(memberUserId);
        assertEquals(1, createdMember.plans().size());
        assertEquals(2, createdMember.payments().size());

        // 3. List Members and verify the member's profile, plans, payments, and mappings
        MemberListResponseDTO listResp = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get("/members/list")
                .then()
                .statusCode(200)
                .extract()
                .as(MemberListResponseDTO.class);

        MemberRecordDTO memberRecord = listResp.members().stream()
                .filter(m -> m.userId().equals(memberUserId))
                .findFirst()
                .orElse(null);

        assertNotNull(memberRecord);
        assertEquals("Arnold", memberRecord.firstName());
        assertEquals("Schwarzenegger", memberRecord.lastName());
        assertEquals(2, memberRecord.payments().size());
        assertEquals(1, memberRecord.payments().get(0).plans().size());
        assertEquals("Gold Annual Membership", memberRecord.payments().get(0).plans().get(0).planName());

        // 4. Renew Member: strictly additive renewal
        MemberPlanAssignmentDTO renewalPlan = new MemberPlanAssignmentDTO(
                planId,
                null, // Should auto-compute to endDate + 1 day = 2026-02-01
                LocalDate.of(2026, 2, 28)
        );

        PaymentScheduleDTO renewalPayment = new PaymentScheduleDTO(
                BigDecimal.valueOf(2000.00),
                BigDecimal.valueOf(2000.00),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 1),
                PaymentMethod.UPI,
                "TXN_RENEW_001",
                "Renewal Full Payment"
        );

        MemberRenewRequestDTO renewReq = new MemberRenewRequestDTO(
                memberUserId,
                List.of(renewalPlan),
                List.of(renewalPayment)
        );

        MemberRenewResponseDTO renewResp = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(renewReq)
                .when()
                .post("/members/renew")
                .then()
                .statusCode(200)
                .body("userId", equalTo(memberUserId.toString()))
                .body("renewedPlans", hasSize(1))
                .body("renewedPayments", hasSize(1))
                .extract()
                .as(MemberRenewResponseDTO.class);

        assertEquals(LocalDate.of(2026, 2, 1), renewResp.renewedPlans().get(0).startDate());

        // Verify that past records are intact via list
        MemberListResponseDTO afterRenewList = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get("/members/list")
                .then()
                .statusCode(200)
                .extract()
                .as(MemberListResponseDTO.class);

        MemberRecordDTO renewedMemberRecord = afterRenewList.members().stream()
                .filter(m -> m.userId().equals(memberUserId))
                .findFirst()
                .orElse(null);

        assertNotNull(renewedMemberRecord);
        assertEquals(3, renewedMemberRecord.payments().size(), "Previous 2 installments + 1 renewal payment must all exist");

        // 5. Update Member: Delete-Insert State Reconciliation
        // Update contact and profile name, replace active plans and reconcile unpaid installments
        MemberProfileDTO updatedProfile = new MemberProfileDTO(
                "Arnold",
                "Champion",
                new PhoneNumber("91", phoneNum)
        );

        MemberPlanAssignmentDTO updatedPlan = new MemberPlanAssignmentDTO(
                planId,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        PaymentScheduleDTO updatedInstallment = new PaymentScheduleDTO(
                BigDecimal.valueOf(2200.00),
                BigDecimal.valueOf(2200.00),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1),
                PaymentMethod.CASH,
                "TXN_UPDATED_001",
                "Reconciled Full Payment"
        );

        MemberUpdateRequestDTO updateReq = new MemberUpdateRequestDTO(
                memberUserId,
                updatedProfile,
                List.of(updatedPlan),
                List.of(updatedInstallment)
        );

        MemberUpdateResponseDTO updateResp = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(updateReq)
                .when()
                .post("/members/update")
                .then()
                .statusCode(200)
                .body("userId", equalTo(memberUserId.toString()))
                .body("plans", hasSize(1))
                .body("payments", hasSize(1))
                .extract()
                .as(MemberUpdateResponseDTO.class);

        assertEquals(1, updateResp.plans().size());

        // Verify updated profile reflected in list
        MemberListResponseDTO afterUpdateList = RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get("/members/list")
                .then()
                .statusCode(200)
                .extract()
                .as(MemberListResponseDTO.class);

        MemberRecordDTO updatedMemberRecord = afterUpdateList.members().stream()
                .filter(m -> m.userId().equals(memberUserId))
                .findFirst()
                .orElse(null);

        assertNotNull(updatedMemberRecord);
        assertEquals("Arnold", updatedMemberRecord.firstName());
        assertEquals("Champion", updatedMemberRecord.lastName());
    }

    @Test
    @DisplayName("Create member with duplicate phone number fails with 409 conflict")
    void testCreateMember_duplicatePhoneFailsConflict() {
        String token = getSuperAdminToken();
        String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
        String phoneNum = "97" + suffix.substring(Math.max(0, suffix.length() - 8));
        while (phoneNum.length() < 10) {
            phoneNum += "0";
        }

        MemberCreateRequestDTO req1 = new MemberCreateRequestDTO(
                "Original",
                "User",
                new PhoneNumber("91", phoneNum),
                List.of(),
                List.of()
        );

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(req1)
                .when()
                .post("/members/new")
                .then()
                .statusCode(200);

        // Attempt second create with same phone number
        MemberCreateRequestDTO req2 = new MemberCreateRequestDTO(
                "Duplicate",
                "User",
                new PhoneNumber("91", phoneNum),
                List.of(),
                List.of()
        );

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(req2)
                .when()
                .post("/members/new")
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("Create member with invalid phone number fails validation")
    void testCreateMember_invalidPhoneNumberFailsValidation() {
        String token = getSuperAdminToken();

        MemberCreateRequestDTO request = new MemberCreateRequestDTO(
                "Invalid",
                "Phone",
                new PhoneNumber("91", "123"), // too short
                List.of(),
                List.of()
        );

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/members/new")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Create member with missing first or last name fails validation")
    void testCreateMember_missingNamesFailValidation() {
        String token = getSuperAdminToken();

        MemberCreateRequestDTO missingFirst = new MemberCreateRequestDTO(
                "",
                "LastName",
                new PhoneNumber("91", "9876543210"),
                List.of(),
                List.of()
        );

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(missingFirst)
                .when()
                .post("/members/new")
                .then()
                .statusCode(400);

        MemberCreateRequestDTO missingLast = new MemberCreateRequestDTO(
                "FirstName",
                "",
                new PhoneNumber("91", "9876543210"),
                List.of(),
                List.of()
        );

        RestAssured
                .given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(missingLast)
                .when()
                .post("/members/new")
                .then()
                .statusCode(400);
    }
}
