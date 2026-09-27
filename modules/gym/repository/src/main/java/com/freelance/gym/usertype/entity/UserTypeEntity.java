package com.freelance.gym.usertype.entity;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "FL_USER_TYPE")
@NamedQueries({
        @NamedQuery(
                name = UserTypeEntity.QUERY_FIND_BY_CODE,
                query = "SELECT u FROM UserTypeEntity u WHERE u.code = :code"
        ),
        @NamedQuery(
                name = UserTypeEntity.QUERY_FIND_ALL,
                query = "SELECT u FROM UserTypeEntity u ORDER BY u.code ASC"
        )
})
public class UserTypeEntity extends AbstractAuditEntity {

    public static final String QUERY_FIND_BY_CODE = "UserTypeEntity.findByCode";
    public static final String QUERY_FIND_ALL = "UserTypeEntity.findAll";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    public UserTypeEntity() {
    }

    public UserTypeEntity(UUID id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
