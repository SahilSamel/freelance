package com.freelance.gym.plan.entity;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "FL_PLAN")
@NamedQueries({
        @NamedQuery(
                name = PlanEntity.QUERY_FIND_BY_NAME,
                query = "SELECT p FROM PlanEntity p WHERE LOWER(p.name) = LOWER(:name)"
        ),
        @NamedQuery(
                name = PlanEntity.QUERY_FIND_ALL,
                query = "SELECT p FROM PlanEntity p ORDER BY p.name ASC"
        ),
        @NamedQuery(
                name = PlanEntity.QUERY_FIND_ALL_ACTIVE,
                query = "SELECT p FROM PlanEntity p WHERE p.isActive = true ORDER BY p.name ASC"
        ),
        @NamedQuery(
                name = PlanEntity.QUERY_FIND_BY_IDS,
                query = "SELECT p FROM PlanEntity p WHERE p.id IN :ids"
        )
})
public class PlanEntity extends AbstractAuditEntity {

    public static final String QUERY_FIND_BY_NAME = "PlanEntity.findByName";
    public static final String QUERY_FIND_ALL = "PlanEntity.findAll";
    public static final String QUERY_FIND_ALL_ACTIVE = "PlanEntity.findAllActive";
    public static final String QUERY_FIND_BY_IDS = "PlanEntity.findByIds";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "default_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal defaultPrice = BigDecimal.ZERO;

    @Column(name = "default_duration", nullable = false)
    private Integer defaultDuration = 30;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public PlanEntity() {
    }

    public PlanEntity(UUID id, String name, String description, BigDecimal defaultPrice, Integer defaultDuration, boolean isActive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultPrice = defaultPrice != null ? defaultPrice : BigDecimal.ZERO;
        this.defaultDuration = defaultDuration != null ? defaultDuration : 30;
        this.isActive = isActive;
    }

    public PlanEntity(UUID id, String name, String description, BigDecimal defaultPrice, boolean isActive) {
        this(id, name, description, defaultPrice, 30, isActive);
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

    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(BigDecimal defaultPrice) {
        this.defaultPrice = defaultPrice != null ? defaultPrice : BigDecimal.ZERO;
    }

    public Integer getDefaultDuration() {
        return defaultDuration;
    }

    public void setDefaultDuration(Integer defaultDuration) {
        this.defaultDuration = defaultDuration != null ? defaultDuration : 30;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
