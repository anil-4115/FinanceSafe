package com.financialfraudassistant.dto;

import com.financialfraudassistant.model.ScamReport;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IncidentResponse(
        Integer id,
        String channel,
        String description,
        BigDecimal amountAtRisk,
        int riskScore,
        LocalDateTime createdAt) {

    public static IncidentResponse from(ScamReport report) {
        return new IncidentResponse(report.getId(), report.getChannel(), report.getDescription(),
                report.getAmountAtRisk(), report.getRiskScore(), report.getCreatedAt());
    }
}