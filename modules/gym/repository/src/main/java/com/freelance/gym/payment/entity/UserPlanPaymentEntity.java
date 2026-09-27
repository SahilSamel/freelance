package com.freelance.gym.payment.entity;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "FL_USER_PLAN_PAYMENT", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_plan_payment", columnNames = {"user_plan_id", "payment_id"})
})
@NamedQueries({
        @NamedQuery(
                name = UserPlanPaymentEntity.QUERY_FIND_BY_USER_PLAN_ID,
                query = "SELECT upp FROM UserPlanPaymentEntity upp WHERE upp.userPlanId = :userPlanId"
        ),
        @NamedQuery(
                name = UserPlanPaymentEntity.QUERY_FIND_BY_PAYMENT_ID,
                query = "SELECT upp FROM UserPlanPaymentEntity upp WHERE upp.paymentId = :paymentId"
        ),
        @NamedQuery(
                name = UserPlanPaymentEntity.QUERY_FIND_BY_USER_PLAN_IDS,
                query = "SELECT upp FROM UserPlanPaymentEntity upp WHERE upp.userPlanId IN :userPlanIds"
        ),
        @NamedQuery(
                name = UserPlanPaymentEntity.QUERY_FIND_BY_PAYMENT_IDS,
                query = "SELECT upp FROM UserPlanPaymentEntity upp WHERE upp.paymentId IN :paymentIds"
        ),
        @NamedQuery(
                name = UserPlanPaymentEntity.QUERY_DELETE_BY_USER_PLAN_ID,
                query = "DELETE FROM UserPlanPaymentEntity upp WHERE upp.userPlanId = :userPlanId"
        ),
        @NamedQuery(
                name = UserPlanPaymentEntity.QUERY_DELETE_BY_PAYMENT_IDS,
                query = "DELETE FROM UserPlanPaymentEntity upp WHERE upp.paymentId IN :paymentIds"
        )
})
public class UserPlanPaymentEntity extends AbstractAuditEntity {

    public static final String QUERY_FIND_BY_USER_PLAN_ID = "UserPlanPaymentEntity.findByUserPlanId";
    public static final String QUERY_FIND_BY_PAYMENT_ID = "UserPlanPaymentEntity.findByPaymentId";
    public static final String QUERY_FIND_BY_USER_PLAN_IDS = "UserPlanPaymentEntity.findByUserPlanIds";
    public static final String QUERY_FIND_BY_PAYMENT_IDS = "UserPlanPaymentEntity.findByPaymentIds";
    public static final String QUERY_DELETE_BY_USER_PLAN_ID = "UserPlanPaymentEntity.deleteByUserPlanId";
    public static final String QUERY_DELETE_BY_PAYMENT_IDS = "UserPlanPaymentEntity.deleteByPaymentIds";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_plan_id", nullable = false)
    private UUID userPlanId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    public UserPlanPaymentEntity() {
    }

    public UserPlanPaymentEntity(UUID id, UUID userPlanId, UUID paymentId) {
        this.id = id;
        this.userPlanId = userPlanId;
        this.paymentId = paymentId;
    }

    public UserPlanPaymentEntity(UUID userPlanId, UUID paymentId) {
        this(UUID.randomUUID(), userPlanId, paymentId);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserPlanId() {
        return userPlanId;
    }

    public void setUserPlanId(UUID userPlanId) {
        this.userPlanId = userPlanId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }
}
