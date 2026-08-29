package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fraud_indicators")
public class FraudIndicator {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "analysis_id", nullable = false)
    private FraudAnalysis analysis;
    @Column(nullable = false)
    private String kind;
    @Column(nullable = false, length = 2000)
    private String label;
    @Column(nullable = false)
    private Integer weight;

    protected FraudIndicator() { }

    public FraudIndicator(FraudAnalysis analysis, String kind, String label, int weight) {
        this.analysis = analysis;
        this.kind = kind;
        this.label = label;
        this.weight = weight;
    }

    public Integer getId() { return id; }
    public FraudAnalysis getAnalysis() { return analysis; }
    public String getKind() { return kind; }
    public String getLabel() { return label; }
    public Integer getWeight() { return weight; }
}