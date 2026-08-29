package com.freelance.entity.role;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@NamedQueries({
        @NamedQuery(name = Role.QUERY_FIND_ALL, query = "SELECT r FROM Role r"),
        @NamedQuery(name = Role.QUERY_FIND_BY_ID, query = "SELECT r FROM Role r WHERE r.id = :id"),
        @NamedQuery(name = Role.QUERY_FIND_BY_NAME, query = "SELECT r FROM Role r WHERE r.name = :name"),
        @NamedQuery(name = Role.QUERY_COUNT_BY_NAME, query = "SELECT COUNT(r) FROM Role r WHERE r.name = :name"),
        @NamedQuery(name = Role.QUERY_FIND_ALL_SYSTEM_ROLES, query = "SELECT r FROM Role r WHERE r.isSystem = TRUE"),
        @NamedQuery(name = Role.QUERY_DELETE_SYSTEM_ROLES, query = "DELETE FROM Role r WHERE r.isSystem = TRUE")
})
@Table(name = "FL_ROLE", schema = "freelance")
public class Role extends AbstractAuditEntity {

    public static final String QUERY_FIND_ALL = "Role.findAll";
    public static final String QUERY_FIND_BY_ID = "Role.findById";
    public static final String QUERY_FIND_BY_NAME = "Role.findByName";
    public static final String QUERY_COUNT_BY_NAME = "Role.countByName";
    public static final String QUERY_FIND_ALL_SYSTEM_ROLES = "Role.findAllSystemRoles";
    public static final String QUERY_DELETE_SYSTEM_ROLES = "Role.deleteSystemRoles";

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    public Role() {}

    public Role(UUID id, String name, String description, boolean isSystem) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isSystem = isSystem;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }
}