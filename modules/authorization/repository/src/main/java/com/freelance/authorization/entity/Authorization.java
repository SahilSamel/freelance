package com.freelance.authorization.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "FL_AUTHORIZATION", schema = "freelance")
public class Authorization {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "key", nullable = false, unique = true, length = 255)
    private String key;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "group_path", nullable = false, length = 255)
    private String groupPath;

    public Authorization() {}

    public Authorization(UUID id, String key, String name, String description, String groupPath) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.description = description;
        this.groupPath = groupPath;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public String getGroupPath() {
        return groupPath;
    }

    public void setGroupPath(String groupPath) {
        this.groupPath = groupPath;
    }
}