package entity.identity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "freelance")
public class User {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ─── CONSTRUCTORS ────────────────────────────────────────────────────────

    // JPA requires a protected or public no-arg constructor
    public User() {
    }

    public User(String email) {
        this.email = email;
    }

    // ─── LIFECYCLE HOOKS ─────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        // Automatically populate UUID and timestamp right before database insertion
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // ─── GETTERS AND SETTERS ─────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}