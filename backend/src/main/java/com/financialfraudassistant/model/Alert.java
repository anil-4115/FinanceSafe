package com.financialfraudassistant.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name = "alerts", indexes = @Index(name = "idx_alert_user", columnList = "user_id"))
public class Alert {
    public enum Severity { INFO, WARNING, CRITICAL }
    public enum Status { OPEN, RESOLVED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Severity severity;
    @Column(name = "alert_type", nullable = false) private String alertType;
    @Column(name = "risk_score", nullable = false) private Integer riskScore;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.OPEN;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    protected Alert() { }
    public Alert(User user, String title, String message, Severity severity, String alertType, int riskScore) { this.user = user; this.title = title; this.message = message; this.severity = severity; this.alertType = alertType; this.riskScore = riskScore; }
    public void resolve() { status = Status.RESOLVED; resolvedAt = LocalDateTime.now(); }
    public Integer getId() { return id; } public String getTitle() { return title; } public String getMessage() { return message; }
    public Severity getSeverity() { return severity; } public String getAlertType() { return alertType; } public Integer getRiskScore() { return riskScore; }
    public Status getStatus() { return status; } public LocalDateTime getCreatedAt() { return createdAt; }
}
