package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "education_attempts")
public class EducationAttempt {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(optional = false) @JoinColumn(name = "module_id", nullable = false)
    private EducationModule module;
    @Column(name = "score_pct", nullable = false)
    private Integer scorePct;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected EducationAttempt() { }

    public EducationAttempt(User user, EducationModule module, int scorePct) {
        this.user = user;
        this.module = module;
        this.scorePct = scorePct;
    }

    public Integer getId() { return id; }
    public User getUser() { return user; }
    public EducationModule getModule() { return module; }
    public Integer getScorePct() { return scorePct; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}