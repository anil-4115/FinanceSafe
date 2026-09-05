package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "assets")
public class Asset {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20, unique = true)
    private String symbol;

    @Column(nullable = false)
    private String name;

    @Column(name = "asset_type", nullable = false, length = 50)
    private String assetType;

    @Column(nullable = false, length = 100)
    private String sector;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "weekly_volatility", nullable = false, precision = 6, scale = 4)
    private BigDecimal weeklyVolatility;

    protected Asset() { }

    public Asset(String symbol, String name, String assetType, String sector, BigDecimal basePrice, BigDecimal weeklyVolatility) {
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
        this.sector = sector;
        this.basePrice = basePrice;
        this.weeklyVolatility = weeklyVolatility;
    }

    public Integer getId() { return id; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getAssetType() { return assetType; }
    public String getSector() { return sector; }
    public BigDecimal getBasePrice() { return basePrice; }
    public BigDecimal getWeeklyVolatility() { return weeklyVolatility; }
}