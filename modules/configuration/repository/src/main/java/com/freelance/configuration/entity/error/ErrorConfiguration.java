package com.freelance.configuration.entity.error;

import com.freelance.rest.app.repository.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "FL_ERROR_MESSAGE")
public class ErrorConfiguration extends AbstractEntity {

    @Id
    @Column(name = "error_id", nullable = false, length = 255)
    private String errorId;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "description", length = 255)
    private String description;

    public ErrorConfiguration() {
    }

    public ErrorConfiguration(String errorId, int statusCode, String errorMessage, String description) {
        this.errorId = errorId;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.description = description;
    }

    // Getters and Setters
    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
