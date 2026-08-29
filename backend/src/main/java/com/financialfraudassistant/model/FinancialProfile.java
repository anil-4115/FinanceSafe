package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_profile")
public class FinancialProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "age_range") private String ageRange;
    @Column(name = "employment_type") private String employmentType;
    @Column(name = "monthly_income") private BigDecimal monthlyIncome;
    @Column(name = "monthly_fixed_expenses") private BigDecimal monthlyFixedExpenses;
    private BigDecimal savings;
    @Column(name = "existing_investments") private BigDecimal existingInvestments;
    private BigDecimal debt;
    @Column(name = "risk_tolerance") private String riskTolerance;
    @Column(name = "investment_experience") private String investmentExperience;
    @Column(name = "preferred_categories") private String preferredCategories;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    protected FinancialProfile() { }
    public FinancialProfile(User user) { this.user = user; }

    public void update(String ageRange, String employmentType, BigDecimal monthlyIncome, BigDecimal monthlyFixedExpenses,
                       BigDecimal savings, BigDecimal existingInvestments, BigDecimal debt, String riskTolerance,
                       String investmentExperience, String preferredCategories) {
        this.ageRange = ageRange;
        this.employmentType = employmentType;
        this.monthlyIncome = monthlyIncome;
        this.monthlyFixedExpenses = monthlyFixedExpenses;
        this.savings = savings;
        this.existingInvestments = existingInvestments;
        this.debt = debt;
        this.riskTolerance = riskTolerance;
        this.investmentExperience = investmentExperience;
        this.preferredCategories = preferredCategories;
        this.updatedAt = LocalDateTime.now();
    }

    public String getAgeRange() { return ageRange; }
    public String getEmploymentType() { return employmentType; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public BigDecimal getMonthlyFixedExpenses() { return monthlyFixedExpenses; }
    public BigDecimal getSavings() { return savings; }
    public BigDecimal getExistingInvestments() { return existingInvestments; }
    public BigDecimal getDebt() { return debt; }
    public String getRiskTolerance() { return riskTolerance; }
    public String getInvestmentExperience() { return investmentExperience; }
    public String getPreferredCategories() { return preferredCategories; }
}
