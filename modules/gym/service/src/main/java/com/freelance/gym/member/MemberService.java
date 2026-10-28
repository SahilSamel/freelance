package com.freelance.gym.member;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertsEngine;
import com.freelance.alerts.templates.MemberRenewalConfirmationTemplate;
import com.freelance.alerts.templates.MemberWelcomeOnboardingTemplate;
import com.freelance.alerts.dto.UserContactDTO;
import com.freelance.alerts.preference.service.UserContactService;
import com.freelance.gym.credit.dto.MemberCreditDTO;
import com.freelance.gym.credit.entity.MemberCreditEntity;
import com.freelance.gym.credit.repository.MemberCreditRepository;
import com.freelance.gym.referral.dto.MemberReferralDTO;
import com.freelance.gym.referral.entity.MemberReferralEntity;
import com.freelance.gym.referral.repository.MemberReferralRepository;
import com.freelance.gym.payment.ReceiptNotificationService;
import com.freelance.gym.member.assembler.MemberAssembler;
import com.freelance.gym.member.constraint.MemberCreateConstraint;
import com.freelance.gym.member.constraint.MemberRenewConstraint;
import com.freelance.gym.member.constraint.MemberUpdateConstraint;
import com.freelance.gym.member.dto.*;
import com.freelance.gym.member.validation.MemberCreateValidation;
import com.freelance.gym.member.validation.MemberRenewValidation;
import com.freelance.gym.member.validation.MemberUpdateValidation;
import com.freelance.gym.payment.PaymentService;
import com.freelance.gym.payment.assembler.PaymentAssembler;
import com.freelance.gym.payment.dto.PaymentCollectRequestDTO;
import com.freelance.gym.payment.dto.PaymentReceiptSendRequestDTO;
import com.freelance.gym.payment.dto.PaymentReceiptSendResponseDTO;
import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.entity.UserPlanPaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.gym.payment.repository.UserPlanPaymentRepository;
import com.freelance.gym.plan.PlanService;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.gym.userplan.entity.UserPlanEntity;
import com.freelance.gym.userplan.repository.UserPlanRepository;
import com.freelance.gym.usertype.entity.UserTypeEntity;
import com.freelance.gym.usertype.entity.UserUserTypeEntity;
import com.freelance.gym.usertype.repository.UserTypeRepository;
import com.freelance.gym.usertype.repository.UserUserTypeRepository;
import com.freelance.gym.util.PasswordGenerator;
import com.freelance.identityManagement.PhoneNumber;
import com.freelance.user.UserDTO;
import com.freelance.user.contact.UserContactRequestDTO;
import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.create.UserCreateService;
import com.freelance.user.read.UserReadService;
import com.freelance.user.update.UserUpdateRequestDTO;
import com.freelance.user.update.UserUpdateService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class MemberService {

    private final UserReadService userReadService;
    private final UserCreateService userCreateService;
    private final UserUpdateService userUpdateService;
    private final UserContactService userContactService;
    private final UserTypeRepository userTypeRepository;
    private final UserUserTypeRepository userUserTypeRepository;
    private final UserPlanRepository userPlanRepository;
    private final PaymentRepository paymentRepository;
    private final UserPlanPaymentRepository userPlanPaymentRepository;
    private final PlanRepository planRepository;
    private final PlanService planService;
    private final PaymentService paymentService;
    private final MemberAssembler assembler;
    private final PaymentAssembler paymentAssembler;
    private final ReceiptNotificationService receiptNotificationService;
    private final AlertsEngine alertsEngine;
    private final MemberCreditRepository memberCreditRepository;
    private final MemberReferralRepository memberReferralRepository;

    public MemberService() {
        this(
                new UserReadService(),
                new UserCreateService(),
                new UserUpdateService(),
                new UserContactService(),
                new UserTypeRepository(),
                new UserUserTypeRepository(),
                new UserPlanRepository(),
                new PaymentRepository(),
                new UserPlanPaymentRepository(),
                new PlanRepository(),
                new PlanService(),
                new PaymentService(),
                new MemberAssembler(),
                new PaymentAssembler()
        );
    }

    public MemberService(
            UserReadService userReadService,
            UserCreateService userCreateService,
            UserUpdateService userUpdateService,
            UserContactService userContactService,
            UserTypeRepository userTypeRepository,
            UserUserTypeRepository userUserTypeRepository,
            UserPlanRepository userPlanRepository,
            PaymentRepository paymentRepository,
            UserPlanPaymentRepository userPlanPaymentRepository,
            PlanRepository planRepository,
            PlanService planService,
            PaymentService paymentService,
            MemberAssembler assembler,
            PaymentAssembler paymentAssembler
    ) {
        this(
                userReadService,
                userCreateService,
                userUpdateService,
                userContactService,
                userTypeRepository,
                userUserTypeRepository,
                userPlanRepository,
                paymentRepository,
                userPlanPaymentRepository,
                planRepository,
                planService,
                paymentService,
                assembler,
                paymentAssembler,
                new ReceiptNotificationService(),
                AlertsEngine.getInstance()
        );
    }

    public MemberService(
            UserReadService userReadService,
            UserCreateService userCreateService,
            UserUpdateService userUpdateService,
            UserContactService userContactService,
            UserTypeRepository userTypeRepository,
            UserUserTypeRepository userUserTypeRepository,
            UserPlanRepository userPlanRepository,
            PaymentRepository paymentRepository,
            UserPlanPaymentRepository userPlanPaymentRepository,
            PlanRepository planRepository,
            PlanService planService,
            PaymentService paymentService,
            MemberAssembler assembler,
            PaymentAssembler paymentAssembler,
            ReceiptNotificationService receiptNotificationService,
            AlertsEngine alertsEngine
    ) {
        this(
                userReadService,
                userCreateService,
                userUpdateService,
                userContactService,
                userTypeRepository,
                userUserTypeRepository,
                userPlanRepository,
                paymentRepository,
                userPlanPaymentRepository,
                planRepository,
                planService,
                paymentService,
                assembler,
                paymentAssembler,
                receiptNotificationService,
                alertsEngine,
                new MemberCreditRepository(),
                new MemberReferralRepository()
        );
    }

    public MemberService(
            UserReadService userReadService,
            UserCreateService userCreateService,
            UserUpdateService userUpdateService,
            UserContactService userContactService,
            UserTypeRepository userTypeRepository,
            UserUserTypeRepository userUserTypeRepository,
            UserPlanRepository userPlanRepository,
            PaymentRepository paymentRepository,
            UserPlanPaymentRepository userPlanPaymentRepository,
            PlanRepository planRepository,
            PlanService planService,
            PaymentService paymentService,
            MemberAssembler assembler,
            PaymentAssembler paymentAssembler,
            ReceiptNotificationService receiptNotificationService,
            AlertsEngine alertsEngine,
            MemberCreditRepository memberCreditRepository,
            MemberReferralRepository memberReferralRepository
    ) {
        this.userReadService = userReadService;
        this.userCreateService = userCreateService;
        this.userUpdateService = userUpdateService;
        this.userContactService = userContactService;
        this.userTypeRepository = userTypeRepository;
        this.userUserTypeRepository = userUserTypeRepository;
        this.userPlanRepository = userPlanRepository;
        this.paymentRepository = paymentRepository;
        this.userPlanPaymentRepository = userPlanPaymentRepository;
        this.planRepository = planRepository;
        this.planService = planService;
        this.paymentService = paymentService;
        this.assembler = assembler;
        this.paymentAssembler = paymentAssembler;
        this.receiptNotificationService = receiptNotificationService;
        this.alertsEngine = alertsEngine;
        this.memberCreditRepository = memberCreditRepository;
        this.memberReferralRepository = memberReferralRepository;
    }

    public MemberCreateResponseDTO createMember(MemberCreateRequestDTO request) {
        new MemberCreateValidation().validateRequest(request);
        new MemberCreateConstraint(userReadService, userContactService).checkConstraints(request);

        // 1. Resolve username
        String username = "mem_" + request.phoneNumber().number();

        // 2. Generate secure temporary password
        String generatedPassword = PasswordGenerator.generateRandomPassword();

        // 3. Prepare contacts
        List<UserContactRequestDTO> contacts = new ArrayList<>();
        if (request.phoneNumber() != null) {
            contacts.add(new UserContactRequestDTO(AlertChannel.WHATSAPP, request.phoneNumber().toE164()));
        }

        // 4. Create user via UserCreateService
        var userResponse = userCreateService.createUser(new UserCreateRequestDTO(
                username,
                generatedPassword,
                request.firstName(),
                request.lastName(),
                List.of(),
                contacts
        ));
        UUID userId = userResponse.id();

        // 5. Assign MEMBER UserType
        Optional<UserTypeEntity> memberTypeOpt = userTypeRepository.findByCode("MEMBER");
        if (memberTypeOpt.isPresent()) {
            UserUserTypeEntity userUserType = new UserUserTypeEntity(userId, memberTypeOpt.get().getId());
            userUserTypeRepository.persist(userUserType);
        }

        // 6. If plans & payments are provided, assign them; otherwise, complete as Prospect Member
        List<UserPlanResponseDTO> assignedPlans = Collections.emptyList();
        List<PaymentResponseDTO> assignedPayments = Collections.emptyList();

        if (request.plans() != null && !request.plans().isEmpty()) {
            assignedPlans = planService.assignPlansToUser(userId, request.plans());

            if (request.payments() != null && !request.payments().isEmpty()) {
                assignedPayments = paymentService.createInstallments(assignedPlans, request.payments());
            }

            // 7. Sequentially emit onboarding alerts for active member
            alertsEngine.emitAlert(new MemberWelcomeOnboardingTemplate(), userId, buildPlansBreakdownCustomParameters(assignedPlans));

            // If an upfront payment was made during creation, send receipt right after
            if (assignedPayments != null) {
                final List<UserPlanResponseDTO> finalPlans = assignedPlans;
                assignedPayments.stream()
                        .filter(p -> p.paidAmount() != null && p.paidAmount().compareTo(BigDecimal.ZERO) > 0)
                        .findFirst()
                        .ifPresent(p -> receiptNotificationService.generateAndDispatchReceipt(
                                userId,
                                p.id(),
                                p.paidAmount(),
                                p.paymentMethod() != null ? p.paymentMethod().name() : "UPI",
                                p.transactionId(),
                                p.paidDate(),
                                finalPlans
                        ));
            }
        }

        return new MemberCreateResponseDTO(
                userId,
                username,
                generatedPassword,
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                List.of("MEMBER"),
                assignedPlans,
                assignedPayments
        );
    }

    public MemberRenewResponseDTO renewMember(MemberRenewRequestDTO request) {
        new MemberRenewValidation().validateRequest(request);
        new MemberRenewConstraint(userReadService, userPlanRepository).checkConstraints(request);

        UUID userId = request.userId();

        // 1. Calculate renewal dates (default next start date = previousPlan.endDate + 1 day)
        List<MemberPlanAssignmentDTO> adjustedPlans = new ArrayList<>();
        for (MemberPlanAssignmentDTO plan : request.plans()) {
            LocalDate startDate = plan.startDate();
            if (startDate == null) {
                Optional<UserPlanEntity> latest = userPlanRepository.findLatestByUserAndPlan(userId, plan.planId());
                if (latest.isPresent() && latest.get().getEndDate() != null) {
                    LocalDate nextDay = latest.get().getEndDate().plusDays(1);
                    startDate = nextDay.isAfter(LocalDate.now()) ? nextDay : LocalDate.now();
                } else {
                    startDate = LocalDate.now();
                }
            }
            LocalDate endDate = plan.endDate();

            adjustedPlans.add(new MemberPlanAssignmentDTO(
                    plan.planId(),
                    startDate,
                    endDate
            ));
        }

        // 2. Check if member was a prospect (0 existing plans)
        List<UserPlanEntity> existingPlans = userPlanRepository.findByUserId(userId);
        boolean isProspect = existingPlans == null || existingPlans.isEmpty();

        // 3. Append renewed plans (strictly additive)
        List<UserPlanResponseDTO> renewedPlans = planService.assignPlansToUser(userId, adjustedPlans);

        // 4. Append new installment schedules (strictly additive)
        List<PaymentResponseDTO> newPayments = Collections.emptyList();
        if (request.payments() != null && !request.payments().isEmpty()) {
            newPayments = paymentService.createInstallments(renewedPlans, request.payments());
        }

        // 5. Sequentially emit welcome alert if prospect or renewal confirmation if existing member
        if (isProspect) {
            alertsEngine.emitAlert(new MemberWelcomeOnboardingTemplate(), userId, buildPlansBreakdownCustomParameters(renewedPlans));
        } else {
            alertsEngine.emitAlert(new MemberRenewalConfirmationTemplate(), userId, buildPlansBreakdownCustomParameters(renewedPlans));
        }

        // If an upfront payment was made during renewal, send receipt right after
        if (newPayments != null) {
            final List<UserPlanResponseDTO> finalRenewedPlans = renewedPlans;
            newPayments.stream()
                    .filter(p -> p.paidAmount() != null && p.paidAmount().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst()
                    .ifPresent(p -> receiptNotificationService.generateAndDispatchReceipt(
                            userId,
                            p.id(),
                            p.paidAmount(),
                            p.paymentMethod() != null ? p.paymentMethod().name() : "UPI",
                            p.transactionId(),
                            p.paidDate(),
                            finalRenewedPlans
                    ));
        }

        return new MemberRenewResponseDTO(userId, renewedPlans, newPayments);
    }

    public MemberListResponseDTO listMembers() {
        // 1. Resolve 'MEMBER' user type
        Optional<UserTypeEntity> memberTypeOpt = userTypeRepository.findByCode("MEMBER");
        if (memberTypeOpt.isEmpty()) {
            return new MemberListResponseDTO(List.of());
        }

        // 2. Query user IDs having 'MEMBER' user type
        List<UUID> memberUserIds = userUserTypeRepository.findUserIdsByUserTypeId(memberTypeOpt.get().getId());
        if (memberUserIds.isEmpty()) {
            return new MemberListResponseDTO(List.of());
        }

        // 3. Query only member users via UserReadService
        List<UserDTO> users = userReadService.readUsersByIds(memberUserIds);
        if (users.isEmpty()) {
            return new MemberListResponseDTO(List.of());
        }

        // 4. Query contacts for member users via UserContactService
        List<UserContactDTO> memberContacts = userContactService.findByUserIds(memberUserIds);
        Map<UUID, List<UserContactDTO>> contactsByUser = memberContacts.stream()
                .collect(Collectors.groupingBy(UserContactDTO::userId));

        // 5. Query plans for member users and build UserPlanResponseDTOs
        List<UserPlanEntity> userPlans = userPlanRepository.findByUserIds(memberUserIds);
        List<PlanEntity> allPlans = planRepository.findAllPlans();
        Map<UUID, String> planNameMap = allPlans.stream()
                .collect(Collectors.toMap(PlanEntity::getId, PlanEntity::getName));

        Map<UUID, UserPlanResponseDTO> userPlanDtoById = new HashMap<>();
        Map<UUID, UUID> userIdByUserPlanId = new HashMap<>();
        Map<UUID, List<UserPlanResponseDTO>> plansByUser = new HashMap<>();
        for (UserPlanEntity up : userPlans) {
            userIdByUserPlanId.put(up.getId(), up.getUserId());
            UserPlanResponseDTO dto = new UserPlanResponseDTO(
                    up.getId(),
                    up.getUserId(),
                    up.getPlanId(),
                    planNameMap.getOrDefault(up.getPlanId(), "Unknown Plan"),
                    up.getStartDate(),
                    up.getEndDate()
            );
            userPlanDtoById.put(up.getId(), dto);
            plansByUser.computeIfAbsent(up.getUserId(), k -> new ArrayList<>()).add(dto);
        }

        // 6. Query payments and map them to their associated plans (Level 2 Payment -> Level 3 Plans)
        List<UUID> allUserPlanIds = userPlans.stream().map(UserPlanEntity::getId).toList();
        List<UserPlanPaymentEntity> mappings = allUserPlanIds.isEmpty() ? List.of() : userPlanPaymentRepository.findByUserPlanIds(allUserPlanIds);
        List<UUID> paymentIds = mappings.stream().map(UserPlanPaymentEntity::getPaymentId).distinct().toList();
        List<PaymentEntity> payments = paymentIds.isEmpty() ? List.of() : paymentRepository.findByIds(paymentIds);
        Map<UUID, PaymentEntity> paymentById = payments.stream().collect(Collectors.toMap(PaymentEntity::getId, p -> p, (a, b) -> a));

        Map<UUID, List<UserPlanResponseDTO>> plansByPaymentId = new HashMap<>();
        for (UserPlanPaymentEntity m : mappings) {
            UserPlanResponseDTO planDto = userPlanDtoById.get(m.getUserPlanId());
            if (planDto != null) {
                plansByPaymentId.computeIfAbsent(m.getPaymentId(), k -> new ArrayList<>()).add(planDto);
            }
        }

        Map<UUID, List<PaymentResponseDTO>> paymentsByUser = new HashMap<>();
        Map<UUID, Set<UUID>> seenPaymentsByUser = new HashMap<>();
        for (UserPlanPaymentEntity m : mappings) {
            UUID uid = userIdByUserPlanId.get(m.getUserPlanId());
            PaymentEntity p = paymentById.get(m.getPaymentId());
            if (uid != null && p != null) {
                if (seenPaymentsByUser.computeIfAbsent(uid, k -> new HashSet<>()).add(p.getId())) {
                    paymentsByUser.computeIfAbsent(uid, k -> new ArrayList<>()).add(
                            paymentAssembler.fromEntity(p, plansByPaymentId.getOrDefault(p.getId(), List.of()))
                    );
                }
            }
        }

        // 7. Query Credits & Referrals
        List<MemberCreditEntity> credits = memberCreditRepository.findByUserIds(memberUserIds);
        Map<UUID, MemberCreditDTO> creditByUser = credits.stream()
                .collect(Collectors.toMap(
                        MemberCreditEntity::getUserId,
                        c -> new MemberCreditDTO(c.getBalance(), c.getNotes()),
                        (a, b) -> a
                ));

        List<MemberReferralEntity> referrals = memberReferralRepository.findByRefereeIds(memberUserIds);
        Map<UUID, UUID> referrerIdByReferee = referrals.stream()
                .collect(Collectors.toMap(MemberReferralEntity::getRefereeId, MemberReferralEntity::getReferrerId, (a, b) -> a));

        List<UUID> allReferrerIds = referrals.stream().map(MemberReferralEntity::getReferrerId).distinct().toList();
        Map<UUID, String> referrerNameById = new HashMap<>();
        if (!allReferrerIds.isEmpty()) {
            List<UserDTO> referrerUsers = userReadService.readUsersByIds(allReferrerIds);
            for (UserDTO ru : referrerUsers) {
                String fullName = ((ru.firstName() != null ? ru.firstName() : "") + " " + (ru.lastName() != null ? ru.lastName() : "")).trim();
                referrerNameById.put(ru.id(), fullName.isEmpty() ? ru.username() : fullName);
            }
        }

        Map<UUID, MemberReferralDTO> referralByUser = new HashMap<>();
        for (MemberReferralEntity r : referrals) {
            referralByUser.put(r.getRefereeId(), new MemberReferralDTO(
                    r.getReferrerId(),
                    referrerNameById.getOrDefault(r.getReferrerId(), "Unknown"),
                    r.getReferralDate(),
                    r.getNotes()
            ));
        }

        // 8. Assemble member records: Level 1 (User) -> Level 2 (Payments) -> Level 3 (Plans)
        List<MemberRecordDTO> records = new ArrayList<>();
        for (UserDTO user : users) {
            List<UserContactDTO> userContacts = contactsByUser.getOrDefault(user.id(), List.of());
            PhoneNumber phone = null;
            for (UserContactDTO c : userContacts) {
                if (c.channel() == AlertChannel.WHATSAPP && c.contactValue() != null) {
                    phone = parsePhoneNumber(c.contactValue());
                }
            }

            records.add(assembler.toMemberRecord(
                    user,
                    phone,
                    user.firstName(),
                    user.lastName(),
                    plansByUser.getOrDefault(user.id(), List.of()),
                    paymentsByUser.getOrDefault(user.id(), List.of()),
                    creditByUser.get(user.id()),
                    referralByUser.get(user.id())
            ));
        }

        return new MemberListResponseDTO(records);
    }

    public MemberUpdateResponseDTO updateMember(MemberUpdateRequestDTO request) {
        new MemberUpdateValidation().validateRequest(request);
        new MemberUpdateConstraint(userReadService).checkConstraints(request);

        UUID userId = request.userId();

        // 1. Update contact details and profile name if provided via UserUpdateService
        if (request.profile() != null) {
            MemberProfileDTO profile = request.profile();
            List<UserContactRequestDTO> contacts = null;
            if (profile.phoneNumber() != null) {
                contacts = List.of(new UserContactRequestDTO(AlertChannel.WHATSAPP, profile.phoneNumber().toE164()));
            }

            userUpdateService.updateUser(new UserUpdateRequestDTO(
                    userId,
                    null,
                    null,
                    profile.firstName(),
                    profile.lastName(),
                    contacts
            ));
        }

        // 1b. Update or delete Member Credit
        if (request.credit() != null) {
            Optional<MemberCreditEntity> existingCredit = memberCreditRepository.findByUserId(userId);
            if (existingCredit.isPresent()) {
                MemberCreditEntity credit = existingCredit.get();
                credit.setBalance(request.credit().balance());
                credit.setNotes(request.credit().notes());
                memberCreditRepository.merge(credit);
            } else {
                memberCreditRepository.persist(new MemberCreditEntity(
                        userId,
                        request.credit().balance(),
                        request.credit().notes()
                ));
            }
        }

        // 1c. Update or delete Member Referral
        if (request.referral() != null) {
            if (request.referral().referrerId() != null) {
                Optional<MemberReferralEntity> existingRef = memberReferralRepository.findByRefereeId(userId);
                if (existingRef.isPresent()) {
                    MemberReferralEntity ref = existingRef.get();
                    ref.setReferrerId(request.referral().referrerId());
                    ref.setReferralDate(request.referral().referralDate() != null ? request.referral().referralDate() : LocalDate.now());
                    ref.setNotes(request.referral().notes());
                    memberReferralRepository.merge(ref);
                } else {
                    memberReferralRepository.persist(new MemberReferralEntity(
                            request.referral().referrerId(),
                            userId,
                            request.referral().referralDate() != null ? request.referral().referralDate() : LocalDate.now(),
                            request.referral().notes()
                    ));
                }
            } else {
                // If referrerId is null, remove referral mapping
                memberReferralRepository.deleteByRefereeId(userId);
            }
        }

        // 2. State Reconciliation (Delete-Insert for Plans & Pending Installments)
        List<UserPlanEntity> oldUserPlans = userPlanRepository.findByUserId(userId);
        List<UUID> oldUserPlanIds = oldUserPlans.stream().map(UserPlanEntity::getId).toList();

        // Track exact old plan associations for existing payments
        List<UserPlanPaymentEntity> oldMappings = oldUserPlanIds.isEmpty()
                ? List.of()
                : userPlanPaymentRepository.findByUserPlanIds(oldUserPlanIds);
        Map<UUID, Set<UUID>> oldPlansByPaymentId = new HashMap<>();
        for (UserPlanPaymentEntity m : oldMappings) {
            oldPlansByPaymentId.computeIfAbsent(m.getPaymentId(), k -> new HashSet<>()).add(m.getUserPlanId());
        }

        Map<UUID, Integer> oldPlanIndexMap = new HashMap<>();
        for (int i = 0; i < oldUserPlans.size(); i++) {
            oldPlanIndexMap.put(oldUserPlans.get(i).getId(), i);
        }

        // Query existing PAID payments before deleting old user plans/mappings
        List<PaymentEntity> paidPayments = paymentService.findPaidPaymentsByUserPlanIds(oldUserPlanIds);

        // Delete unpaid pending installments and mappings
        paymentService.deletePendingPaymentsByUserPlanIds(oldUserPlanIds);

        // Clear active user plans (cascades to delete old mappings in FL_USER_PLAN_PAYMENT)
        userPlanRepository.deleteByUserId(userId);

        List<UserPlanResponseDTO> newPlans = Collections.emptyList();
        if (request.plans() != null && !request.plans().isEmpty()) {
            newPlans = planService.assignPlansToUser(userId, request.plans());
        }

        List<PaymentResponseDTO> newPayments = Collections.emptyList();
        if (request.payments() != null && !request.payments().isEmpty()) {
            newPayments = paymentService.createInstallments(newPlans, request.payments());
        }

        // Relink existing PAID payments specifically to their matching plans
        if (request.paidPaymentPlanLinks() != null && !request.paidPaymentPlanLinks().isEmpty()) {
            paymentService.linkPaidPaymentsPrecisely(
                    paidPayments,
                    newPlans,
                    oldUserPlans,
                    oldPlansByPaymentId,
                    oldPlanIndexMap,
                    request.paidPaymentPlanLinks()
            );
        } else {
            paymentService.linkPaidPaymentsPrecisely(
                    paidPayments,
                    newPlans,
                    oldUserPlans,
                    oldPlansByPaymentId,
                    oldPlanIndexMap
            );
        }

        // If member transitioned from prospect (0 plans) to enrolled (has plans), emit welcome alert and receipt
        if (oldUserPlans.isEmpty() && !newPlans.isEmpty()) {
            alertsEngine.emitAlert(new MemberWelcomeOnboardingTemplate(), userId, buildPlansBreakdownCustomParameters(newPlans));

            if (newPayments != null && !newPayments.isEmpty()) {
                final List<UserPlanResponseDTO> finalPlans = newPlans;
                newPayments.stream()
                        .filter(p -> p.paidAmount() != null && p.paidAmount().compareTo(BigDecimal.ZERO) > 0)
                        .findFirst()
                        .ifPresent(p -> receiptNotificationService.generateAndDispatchReceipt(
                                userId,
                                p.id(),
                                p.paidAmount(),
                                p.paymentMethod() != null ? p.paymentMethod().name() : "UPI",
                                p.transactionId(),
                                p.paidDate(),
                                finalPlans
                        ));
            }
        }

        return new MemberUpdateResponseDTO(userId, newPlans, newPayments);
    }

    public PaymentResponseDTO collectPayment(PaymentCollectRequestDTO request) {
        return paymentService.collectPayment(request);
    }

    public PaymentReceiptSendResponseDTO sendReceipt(PaymentReceiptSendRequestDTO request) {
        return paymentService.sendReceipt(request);
    }

    private Map<String, String> buildPlansBreakdownCustomParameters(List<UserPlanResponseDTO> plans) {
        if (plans == null || plans.isEmpty()) {
            return Map.of();
        }
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        LocalDate today = LocalDate.now();
        StringBuilder breakdown = new StringBuilder();
        for (UserPlanResponseDTO p : plans) {
            String sDate = p.startDate() != null ? p.startDate().format(dateFmt) : "-";
            String eDate = p.endDate() != null ? p.endDate().format(dateFmt) : "-";
            long daysLeft = p.endDate() != null ? ChronoUnit.DAYS.between(today, p.endDate()) : 0;
            String daysStr = daysLeft < 0 ? "(Expired " + Math.abs(daysLeft) + "d ago)" : "(" + daysLeft + " days left)";
            if (!breakdown.isEmpty()) breakdown.append("\n");
            breakdown.append("• ").append(p.planName() != null ? p.planName() : "Gym Service")
                    .append("\n  Period: ").append(sDate).append(" – ").append(eDate).append(" ").append(daysStr);
        }
        return Map.of("MEMBER.plans_breakdown", breakdown.toString());
    }

    private PhoneNumber parsePhoneNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String clean = value.replace("+", "").trim();
            if (clean.length() > 10) {
                String countryCode = clean.substring(0, clean.length() - 10);
                String number = clean.substring(clean.length() - 10);
                return new PhoneNumber(countryCode, number);
            }
            return new PhoneNumber("91", clean);
        } catch (Exception e) {
            return null;
        }
    }
}

