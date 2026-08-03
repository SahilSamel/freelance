package com.freelance.entity.credential;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "FL_USER_CREDENTIALS", schema = "freelance")
public class UserCredential {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    // ─── CONSTRUCTORS ────────────────────────────────────────────────────────

    public UserCredential() {
    }

    public UserCredential(String password) {
        this.password = password;
    }

    // ─── LIFECYCLE HOOKS ─────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    // ─── GETTERS AND SETTERS ─────────────────────────────────────────────────

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