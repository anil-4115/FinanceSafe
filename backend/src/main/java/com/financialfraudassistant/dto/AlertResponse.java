package com.financialfraudassistant.dto;
import com.financialfraudassistant.model.Alert;
import java.time.LocalDateTime;
public record AlertResponse(Integer id, String title, String message, Alert.Severity severity, String alertType,
                            Integer riskScore, Alert.Status status, LocalDateTime createdAt) {
    public static AlertResponse from(Alert alert) { return new AlertResponse(alert.getId(), alert.getTitle(), alert.getMessage(), alert.getSeverity(), alert.getAlertType(), alert.getRiskScore(), alert.getStatus(), alert.getCreatedAt()); }
}
