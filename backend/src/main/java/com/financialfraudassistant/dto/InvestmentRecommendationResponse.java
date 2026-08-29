package com.financialfraudassistant.dto;

import java.math.BigDecimal;
import java.util.List;

public record InvestmentRecommendationResponse(
        String riskProfile,
        int timeHorizonYears,
        String summary,
        List<Allocation> allocations,
        String disclaimer) {

    public record Allocation(String assetClass, int weightPct, BigDecimal amount, String guidance, List<String> exampleProducts) { }
}