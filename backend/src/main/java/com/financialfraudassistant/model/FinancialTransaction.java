package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_transactions")
public class FinancialTransaction {
    public enum Type { INCOME, EXPENSE }
    public enum Source { MANUAL, CSV }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    @Column(nullable = false) private String merchant;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(name = "transaction_type", nullable = false)
    private Type transactionType;
    @Column(nullable = false) private String category;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Source source;
    private String notes;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "risk_score")
    private Integer riskScore;
    @Column(name = "risk_level")
    private String riskLevel;
    @Column(name = "risk_reason", length = 2000)
    private String riskReason;

    protected FinancialTransaction() { }
    public FinancialTransaction(User user, LocalDate transactionDate, String merchant, BigDecimal amount, Type transactionType,
                                String category, Source source, String notes) {
        this.user = user; this.transactionDate = transactionDate; this.merchant = merchant; this.amount = amount;
        this.transactionType = transactionType; this.category = category; this.source = source; this.notes = notes;
    }
    public void applyRisk(Integer score, String level, String reason) {
        this.riskScore = score;
        this.riskLevel = level;
        this.riskReason = reason;
    }
    public void update(LocalDate transactionDate, String merchant, BigDecimal amount, Type transactionType,
                       String category, String notes) {
        this.transactionDate = transactionDate;
        this.merchant = merchant;
        this.amount = amount;
        this.transactionType = transactionType;
        this.category = category;
        this.notes = notes;
        this.riskScore = null;
        this.riskLevel = null;
        this.riskReason = null;
    }
    public Integer getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public String getMerchant() { return merchant; }
    public BigDecimal getAmount() { return amount; }
    public Type getTransactionType() { return transactionType; }
    public String getCategory() { return category; }
    public Source getSource() { return source; }
    public String getNotes() { return notes; }
    public Integer getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public String getRiskReason() { return riskReason; }
}
