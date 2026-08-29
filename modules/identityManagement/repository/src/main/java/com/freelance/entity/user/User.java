package com.freelance.entity.user;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@NamedQueries({
        @NamedQuery(name = User.QUERY_FIND_BY_USERNAME, query = "SELECT u FROM User u WHERE u.username = :username"),
        @NamedQuery(name = User.QUERY_FIND_BY_ID, query = "SELECT u FROM User u WHERE u.id = :id"),
        @NamedQuery(name = User.QUERY_COUNT_BY_USERNAME, query = "SELECT COUNT(u) FROM User u WHERE u.username = :username")
})
@Table(name = "FL_USERS", schema = "freelance")
public class User extends AbstractAuditEntity {

    public static final String QUERY_FIND_BY_USERNAME = "User.findByUsername";
    public static final String QUERY_FIND_BY_ID = "User.findById";
    public static final String QUERY_COUNT_BY_USERNAME = "User.countByUsername";

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 255)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(UUID id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    @PrePersist
    protected void onCreateUser() {
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}