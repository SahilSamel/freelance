package com.freelance.user.entity;

import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@NamedQueries({
        @NamedQuery(name = User.QUERY_FIND_BY_USERNAME, query = "SELECT u FROM User u WHERE u.username = :username"),
        @NamedQuery(name = User.QUERY_FIND_BY_ID, query = "SELECT u FROM User u WHERE u.id = :id"),
        @NamedQuery(name = User.QUERY_COUNT_BY_USERNAME, query = "SELECT COUNT(u) FROM User u WHERE u.username = :username"),
        @NamedQuery(name = User.QUERY_COUNT_ALL, query = "SELECT COUNT(u) FROM User u")
})
@Table(name = "FL_USERS", schema = "freelance")
public class User extends AbstractAuditEntity {

    public static final String QUERY_FIND_BY_USERNAME = "User.findByUsername";
    public static final String QUERY_FIND_BY_ID = "User.findById";
    public static final String QUERY_COUNT_BY_USERNAME = "User.countByUsername";
    public static final String QUERY_COUNT_ALL = "User.countAll";

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 255)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private List<UserContactEntity> contacts = new ArrayList<>();

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

    public User(String username, String password, List<UserContactEntity> contacts) {
        this.username = username;
        this.password = password;
        this.contacts = contacts != null ? contacts : new ArrayList<>();
    }

    public User(UUID id, String username, String password, List<UserContactEntity> contacts) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.contacts = contacts != null ? contacts : new ArrayList<>();
    }

    @PrePersist
    protected void onCreateUser() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.contacts != null) {
            for (UserContactEntity contact : this.contacts) {
                if (contact != null && contact.getUserId() == null) {
                    contact.setUserId(this.id);
                }
            }
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

    public List<UserContactEntity> getContacts() {
        if (contacts == null) {
            contacts = new ArrayList<>();
        }
        return contacts;
    }

    public void setContacts(List<UserContactEntity> contacts) {
        this.contacts = contacts != null ? contacts : new ArrayList<>();
    }

    public void addContact(UserContactEntity contact) {
        if (contact != null) {
            if (this.id != null && contact.getUserId() == null) {
                contact.setUserId(this.id);
            }
            getContacts().add(contact);
        }
    }

    public void removeContact(UserContactEntity contact) {
        if (contact != null) {
            getContacts().remove(contact);
        }
    }
}
