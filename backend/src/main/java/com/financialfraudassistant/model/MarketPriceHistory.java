package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "market_price_history",
        uniqueConstraints = @UniqueConstraint(name = "uk_market_price_asset_date", columnNames = {"asset_id", "price_date"}))
public class MarketPriceHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected MarketPriceHistory() { }

    public MarketPriceHistory(Asset asset, LocalDate priceDate, BigDecimal price) {
        this.asset = asset;
        this.priceDate = priceDate;
        this.price = price;
    }

    public Integer getId() { return id; }
    public Asset getAsset() { return asset; }
    public LocalDate getPriceDate() { return priceDate; }
    public BigDecimal getPrice() { return price; }
}