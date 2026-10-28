package com.freelance.gym.referral.entity;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "FL_MEMBER_REFERRAL")
@NamedQueries({
        @NamedQuery(
                name = MemberReferralEntity.QUERY_FIND_BY_REFEREE_ID,
                query = "SELECT r FROM MemberReferralEntity r WHERE r.refereeId = :refereeId"
        ),
        @NamedQuery(
                name = MemberReferralEntity.QUERY_FIND_BY_REFEREE_IDS,
                query = "SELECT r FROM MemberReferralEntity r WHERE r.refereeId IN :refereeIds"
        ),
        @NamedQuery(
                name = MemberReferralEntity.QUERY_FIND_BY_REFERRER_ID,
                query = "SELECT r FROM MemberReferralEntity r WHERE r.referrerId = :referrerId ORDER BY r.referralDate DESC"
        ),
        @NamedQuery(
                name = MemberReferralEntity.QUERY_DELETE_BY_REFEREE_ID,
                query = "DELETE FROM MemberReferralEntity r WHERE r.refereeId = :refereeId"
        )
})
public class MemberReferralEntity extends AbstractAuditEntity {

    public static final String QUERY_FIND_BY_REFEREE_ID = "MemberReferralEntity.findByRefereeId";
    public static final String QUERY_FIND_BY_REFEREE_IDS = "MemberReferralEntity.findByRefereeIds";
    public static final String QUERY_FIND_BY_REFERRER_ID = "MemberReferralEntity.findByReferrerId";
    public static final String QUERY_DELETE_BY_REFEREE_ID = "MemberReferralEntity.deleteByRefereeId";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "referrer_id", nullable = false)
    private UUID referrerId;

    @Column(name = "referee_id", nullable = false, unique = true)
    private UUID refereeId;

    @Column(name = "referral_date", nullable = false)
    private LocalDate referralDate;

    @Column(name = "notes")
    private String notes;

    public MemberReferralEntity() {
    }

    public MemberReferralEntity(UUID id, UUID referrerId, UUID refereeId, LocalDate referralDate, String notes) {
        this.id = id;
        this.referrerId = referrerId;
        this.refereeId = refereeId;
        this.referralDate = referralDate != null ? referralDate : LocalDate.now();
        this.notes = notes;
    }

    public MemberReferralEntity(UUID referrerId, UUID refereeId, LocalDate referralDate, String notes) {
        this(UUID.randomUUID(), referrerId, refereeId, referralDate, notes);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getReferrerId() {
        return referrerId;
    }

    public void setReferrerId(UUID referrerId) {
        this.referrerId = referrerId;
    }

    public UUID getRefereeId() {
        return refereeId;
    }

    public void setRefereeId(UUID refereeId) {
        this.refereeId = refereeId;
    }

    public LocalDate getReferralDate() {
        return referralDate;
    }

    public void setReferralDate(LocalDate referralDate) {
        this.referralDate = referralDate != null ? referralDate : LocalDate.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
