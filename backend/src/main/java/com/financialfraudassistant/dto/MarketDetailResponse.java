package com.financialfraudassistant.dto;

import java.math.BigDecimal;
import java.util.List;

public record MarketDetailResponse(
        String symbol,
        String name,
        String sector,
        List<PricePoint> history,
        String trend,
        BigDecimal volatilityPct,
        String riskLevel,
        BigDecimal changePct,
        List<String> rationale,
        String disclaimer) {

    public record PricePoint(String date, BigDecimal price) { }
}