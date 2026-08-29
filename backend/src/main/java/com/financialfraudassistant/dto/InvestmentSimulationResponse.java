package com.financialfraudassistant.dto;

import java.math.BigDecimal;
import java.util.List;

public record InvestmentSimulationResponse(
        BigDecimal totalContribution,
        BigDecimal projectedValue,
        BigDecimal totalGain,
        List<YearPoint> series,
        String disclaimer) {

    public record YearPoint(int year, BigDecimal contributed, BigDecimal value, BigDecimal gain) { }
}