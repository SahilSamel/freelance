package entity.credential;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "user_credentials", schema = "freelance")
public class UserCredential {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    // ─── CONSTRUCTORS ────────────────────────────────────────────────────────

    // JPA requires a protected or public no-arg constructor
    public UserCredential() {
    }

    public UserCredential(UUID userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    // ─── LIFECYCLE HOOKS ─────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        // Automatically populate UUID right before database insertion if not manually set
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}