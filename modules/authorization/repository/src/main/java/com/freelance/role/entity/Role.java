package com.freelance.role.entity;

import com.freelance.rest.app.repository.entity.AbstractEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "FL_ROLE", schema = "freelance")
public class Role extends AbstractEntity {

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

    // Getters and Setters
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