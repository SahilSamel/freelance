package com.freelance.entity.user;

import com.freelance.entity.credential.UserCredential;
import com.freelance.rest.app.repository.entity.AbstractEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@NamedQueries({
        @NamedQuery(name = "User.findByUsername", query = "SELECT u FROM User u WHERE u.username = :username")
})
@Table(name = "FL_USERS", schema = "freelance")
public class User extends AbstractEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_credentials_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_fl_users_credentials")
    )
    private UserCredential userCredential;

    // ─── CONSTRUCTORS ────────────────────────────────────────────────────────

    public User() {
    }

    public User(String email) {
        this.email = email;
    }

    // ─── LIFECYCLE HOOKS ─────────────────────────────────────────────────────

    @PrePersist
    protected void onCreateUser() {
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserCredential getUserCredential() {
        return userCredential;
    }

    public void setUserCredential(UserCredential userCredential) {
        this.userCredential = userCredential;
    }

    public String getPassword() {
        return this.userCredential != null ? this.userCredential.getPassword() : null;
    }
}