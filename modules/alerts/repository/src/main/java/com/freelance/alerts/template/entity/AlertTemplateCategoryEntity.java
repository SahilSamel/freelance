package com.freelance.alerts.template.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "FL_ALERT_TEMPLATE_CATEGORY", schema = "freelance")
@NamedQueries({
        @NamedQuery(
                name = "AlertTemplateCategory.findByKey",
                query = "SELECT c FROM AlertTemplateCategoryEntity c WHERE UPPER(c.categoryKey) = UPPER(:categoryKey)"
        ),
        @NamedQuery(
                name = "AlertTemplateCategory.findAll",
                query = "SELECT c FROM AlertTemplateCategoryEntity c ORDER BY c.categoryKey ASC"
        )
})
public class AlertTemplateCategoryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "category_key", nullable = false, unique = true, length = 100)
    private String categoryKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    public AlertTemplateCategoryEntity() {}

    public AlertTemplateCategoryEntity(UUID id, String categoryKey, String name, String description, String priority) {
        this.id = id;
        this.categoryKey = categoryKey;
        this.name = name;
        this.description = description;
        this.priority = priority != null ? priority : "NORMAL";
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCategoryKey() {
        return categoryKey;
    }

    public void setCategoryKey(String categoryKey) {
        this.categoryKey = categoryKey;
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
