package com.freelance.gym.usertype.entity;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "FL_USER_USER_TYPE", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_user_type", columnNames = {"user_id", "user_type_id"})
})
@NamedQueries({
        @NamedQuery(
                name = UserUserTypeEntity.QUERY_FIND_BY_USER_ID,
                query = "SELECT uut FROM UserUserTypeEntity uut WHERE uut.userId = :userId"
        ),
        @NamedQuery(
                name = UserUserTypeEntity.QUERY_FIND_TYPES_FOR_USERS,
                query = "SELECT uut FROM UserUserTypeEntity uut WHERE uut.userId IN :userIds"
        ),
        @NamedQuery(
                name = UserUserTypeEntity.QUERY_DELETE_BY_USER_ID,
                query = "DELETE FROM UserUserTypeEntity uut WHERE uut.userId = :userId"
        ),
        @NamedQuery(
                name = UserUserTypeEntity.QUERY_FIND_USER_IDS_BY_USER_TYPE_ID,
                query = "SELECT uut.userId FROM UserUserTypeEntity uut WHERE uut.userTypeId = :userTypeId"
        )
})
public class UserUserTypeEntity extends AbstractAuditEntity {

    public static final String QUERY_FIND_BY_USER_ID = "UserUserTypeEntity.findByUserId";
    public static final String QUERY_FIND_TYPES_FOR_USERS = "UserUserTypeEntity.findTypesForUsers";
    public static final String QUERY_DELETE_BY_USER_ID = "UserUserTypeEntity.deleteByUserId";
    public static final String QUERY_FIND_USER_IDS_BY_USER_TYPE_ID = "UserUserTypeEntity.findUserIdsByUserTypeId";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_type_id", nullable = false)
    private UUID userTypeId;

    public UserUserTypeEntity() {
    }

    public UserUserTypeEntity(UUID id, UUID userId, UUID userTypeId) {
        this.id = id;
        this.userId = userId;
        this.userTypeId = userTypeId;
    }

    public UserUserTypeEntity(UUID userId, UUID userTypeId) {
        this(UUID.randomUUID(), userId, userTypeId);
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

    public UUID getUserTypeId() {
        return userTypeId;
    }

    public void setUserTypeId(UUID userTypeId) {
        this.userTypeId = userTypeId;
    }
}
