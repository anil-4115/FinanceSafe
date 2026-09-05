package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "decision_analyses",
        indexes = {
                @Index(name = "idx_decision_analysis_user", columnList = "user_id"),
                @Index(name = "idx_decision_analysis_created", columnList = "created_at")
        })
public class DecisionAnalysis {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "decision_type", nullable = false, length = 50)
    private String decisionType;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "input_text", columnDefinition = "TEXT")
    private String inputText;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false, length = 20)
    private String assessment;

    @Column(name = "health_before")
    private Integer healthBefore;

    @Column(name = "health_after")
    private Integer healthAfter;

    @Column(name = "goal_impact", columnDefinition = "TEXT")
    private String goalImpact;

    @Column(columnDefinition = "TEXT")
    private String reasons;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected DecisionAnalysis() { }

    public DecisionAnalysis(User user, String decisionType, BigDecimal amount, String inputText, int score,
                            String assessment, Integer healthBefore, Integer healthAfter, String goalImpact,
                            String reasons, String recommendations) {
        this.user = user;
        this.decisionType = decisionType;
        this.amount = amount;
        this.inputText = inputText;
        this.score = score;
        this.assessment = assessment;
        this.healthBefore = healthBefore;
        this.healthAfter = healthAfter;
        this.goalImpact = goalImpact;
        this.reasons = reasons;
        this.recommendations = recommendations;
    }

    public Integer getId() { return id; }
    public User getUser() { return user; }
    public String getDecisionType() { return decisionType; }
    public BigDecimal getAmount() { return amount; }
    public String getInputText() { return inputText; }
    public Integer getScore() { return score; }
    public String getAssessment() { return assessment; }
    public Integer getHealthBefore() { return healthBefore; }
    public Integer getHealthAfter() { return healthAfter; }
    public String getGoalImpact() { return goalImpact; }
    public String getReasons() { return reasons; }
    public String getRecommendations() { return recommendations; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}