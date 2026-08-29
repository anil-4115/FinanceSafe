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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fraud_analyses")
public class FraudAnalysis {

    public enum InputType { TEXT, URL }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING) @Column(name = "input_type", nullable = false)
    private InputType inputType;
    @Column(nullable = false, length = 10000)
    private String input;
    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;
    @Column(name = "risk_label", nullable = false)
    private String riskLabel;
    @Column(name = "scam_type")
    private String scamType;
    @Column(nullable = false)
    private String confidence;
    @Column(columnDefinition = "TEXT")
    private String summary;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @OneToMany(mappedBy = "analysis")
    private List<FraudIndicator> indicators = new ArrayList<>();

    protected FraudAnalysis() { }

    public FraudAnalysis(User user, InputType inputType, String input, int riskScore, String riskLabel,
                         String scamType, String confidence, String summary) {
        this.user = user;
        this.inputType = inputType;
        this.input = input;
        this.riskScore = riskScore;
        this.riskLabel = riskLabel;
        this.scamType = scamType;
        this.confidence = confidence;
        this.summary = summary;
    }

    public Integer getId() { return id; }
    public User getUser() { return user; }
    public InputType getInputType() { return inputType; }
    public String getInput() { return input; }
    public Integer getRiskScore() { return riskScore; }
    public String getRiskLabel() { return riskLabel; }
    public String getScamType() { return scamType; }
    public String getConfidence() { return confidence; }
    public String getSummary() { return summary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<FraudIndicator> getIndicators() { return indicators; }
}