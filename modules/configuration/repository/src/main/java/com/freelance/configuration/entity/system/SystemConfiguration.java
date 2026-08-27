package com.freelance.configuration.entity.system;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQueries({
        @NamedQuery(name = SystemConfiguration.QUERY_FIND_ALL, query = "SELECT s FROM SystemConfiguration s"),
        @NamedQuery(name = SystemConfiguration.QUERY_FIND_ALL_IDS, query = "SELECT s.configId FROM SystemConfiguration s"),
        @NamedQuery(name = SystemConfiguration.QUERY_DELETE_SYSTEM_OWNED, query = "DELETE FROM SystemConfiguration s WHERE s.lastUpdatedBy IS NULL OR s.lastUpdatedBy = :systemUserId")
})
@Table(name = "FL_SYSTEM_CONFIGURATION")
public class SystemConfiguration extends AbstractAuditEntity {

    public static final String QUERY_FIND_ALL = "SystemConfiguration.findAll";
    public static final String QUERY_FIND_ALL_IDS = "SystemConfiguration.findAllIds";
    public static final String QUERY_DELETE_SYSTEM_OWNED = "SystemConfiguration.deleteSystemOwned";

    @Id
    @Column(name = "config_id", nullable = false, length = 255)
    private String configId;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "description", length = 255)
    private String description;

    public SystemConfiguration() {
    }

    public SystemConfiguration(String configId, String configValue, String description) {
        this.configId = configId;
        this.configValue = configValue;
        this.description = description;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}