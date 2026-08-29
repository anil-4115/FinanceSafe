package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "financial_products")
public class FinancialProduct {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String category;
    @Column(name = "risk_level", nullable = false)
    private String riskLevel;
    @Column(name = "expected_return", nullable = false)
    private String expectedReturn;
    @Column(nullable = false)
    private String liquidity;
    @Column(name = "min_amount", nullable = false)
    private String minAmount;
    @Column(nullable = false)
    private String tenure;
    @Column(name = "suitable_for", nullable = false, columnDefinition = "TEXT")
    private String suitableFor;
    @Column(columnDefinition = "TEXT")
    private String pros;
    @Column(columnDefinition = "TEXT")
    private String cons;
    @Column(columnDefinition = "TEXT")
    private String description;

    protected FinancialProduct() { }

    public FinancialProduct(String name, String category, String riskLevel, String expectedReturn, String liquidity,
                            String minAmount, String tenure, String suitableFor, String pros, String cons, String description) {
        this.name = name;
        this.category = category;
        this.riskLevel = riskLevel;
        this.expectedReturn = expectedReturn;
        this.liquidity = liquidity;
        this.minAmount = minAmount;
        this.tenure = tenure;
        this.suitableFor = suitableFor;
        this.pros = pros;
        this.cons = cons;
        this.description = description;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getRiskLevel() { return riskLevel; }
    public String getExpectedReturn() { return expectedReturn; }
    public String getLiquidity() { return liquidity; }
    public String getMinAmount() { return minAmount; }
    public String getTenure() { return tenure; }
    public String getSuitableFor() { return suitableFor; }
    public String getPros() { return pros; }
    public String getCons() { return cons; }
    public String getDescription() { return description; }
}