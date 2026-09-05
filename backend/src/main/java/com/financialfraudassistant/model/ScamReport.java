package com.financialfraudassistant.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "scam_reports", indexes = @Index(name = "idx_scam_report_user", columnList = "user_id"))
public class ScamReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false) private String channel;
    @Column(nullable = false, columnDefinition = "TEXT") private String description;
    @Column(name = "amount_at_risk", precision = 12, scale = 2) private BigDecimal amountAtRisk;
    @Column(name = "risk_score", nullable = false) private Integer riskScore;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    protected ScamReport() { }
    public ScamReport(User user, String channel, String description, BigDecimal amountAtRisk, int riskScore) { this.user = user; this.channel = channel; this.description = description; this.amountAtRisk = amountAtRisk; this.riskScore = riskScore; }
    public Integer getId() { return id; }
    public User getUser() { return user; }
    public String getChannel() { return channel; }
    public String getDescription() { return description; }
    public BigDecimal getAmountAtRisk() { return amountAtRisk; }
    public Integer getRiskScore() { return riskScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
