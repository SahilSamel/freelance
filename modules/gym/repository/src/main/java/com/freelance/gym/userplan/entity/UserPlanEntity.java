package com.freelance.gym.userplan.entity;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "FL_USER_PLAN")
@NamedQueries({
        @NamedQuery(
                name = UserPlanEntity.QUERY_FIND_BY_USER_ID,
                query = "SELECT up FROM UserPlanEntity up WHERE up.userId = :userId ORDER BY up.startDate DESC"
        ),
        @NamedQuery(
                name = UserPlanEntity.QUERY_FIND_BY_USER_AND_PLAN,
                query = "SELECT up FROM UserPlanEntity up WHERE up.userId = :userId AND up.planId = :planId ORDER BY up.endDate DESC"
        ),
        @NamedQuery(
                name = UserPlanEntity.QUERY_FIND_BY_USER_IDS,
                query = "SELECT up FROM UserPlanEntity up WHERE up.userId IN :userIds ORDER BY up.startDate DESC"
        ),
        @NamedQuery(
                name = UserPlanEntity.QUERY_DELETE_BY_USER_ID,
                query = "DELETE FROM UserPlanEntity up WHERE up.userId = :userId"
        )
})
public class UserPlanEntity extends AbstractAuditEntity {

    public static final String QUERY_FIND_BY_USER_ID = "UserPlanEntity.findByUserId";
    public static final String QUERY_FIND_BY_USER_AND_PLAN = "UserPlanEntity.findByUserAndPlan";
    public static final String QUERY_FIND_BY_USER_IDS = "UserPlanEntity.findByUserIds";
    public static final String QUERY_DELETE_BY_USER_ID = "UserPlanEntity.deleteByUserId";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    public UserPlanEntity() {
    }

    public UserPlanEntity(UUID id, UUID userId, UUID planId, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.userId = userId;
        this.planId = planId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UserPlanEntity(UUID userId, UUID planId, LocalDate startDate, LocalDate endDate) {
        this(UUID.randomUUID(), userId, planId, startDate, endDate);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
