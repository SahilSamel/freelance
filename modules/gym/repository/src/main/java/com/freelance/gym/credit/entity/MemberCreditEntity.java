package com.freelance.gym.credit.entity;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "FL_MEMBER_CREDIT")
@NamedQueries({
        @NamedQuery(
                name = MemberCreditEntity.QUERY_FIND_BY_USER_ID,
                query = "SELECT c FROM MemberCreditEntity c WHERE c.userId = :userId"
        ),
        @NamedQuery(
                name = MemberCreditEntity.QUERY_FIND_BY_USER_IDS,
                query = "SELECT c FROM MemberCreditEntity c WHERE c.userId IN :userIds"
        ),
        @NamedQuery(
                name = MemberCreditEntity.QUERY_DELETE_BY_USER_ID,
                query = "DELETE FROM MemberCreditEntity c WHERE c.userId = :userId"
        )
})
public class MemberCreditEntity extends AbstractAuditEntity {

    public static final String QUERY_FIND_BY_USER_ID = "MemberCreditEntity.findByUserId";
    public static final String QUERY_FIND_BY_USER_IDS = "MemberCreditEntity.findByUserIds";
    public static final String QUERY_DELETE_BY_USER_ID = "MemberCreditEntity.deleteByUserId";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public MemberCreditEntity() {
    }

    public MemberCreditEntity(UUID id, UUID userId, BigDecimal balance, String notes) {
        this.id = id;
        this.userId = userId;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.notes = notes;
    }

    public MemberCreditEntity(UUID userId, BigDecimal balance, String notes) {
        this(UUID.randomUUID(), userId, balance, notes);
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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
