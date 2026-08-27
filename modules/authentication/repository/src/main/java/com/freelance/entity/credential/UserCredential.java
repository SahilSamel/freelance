package com.freelance.entity.credential;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "FL_USER_CREDENTIALS", schema = "freelance")
public class UserCredential extends AbstractAuditEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    public UserCredential() {
    }

    public UserCredential(String password) {
        this.password = password;
    }

    /**
     * Generates a primary key UUID prior to entity insertion if unassigned.
     */
    @PrePersist
    protected void onCreateCredential() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}