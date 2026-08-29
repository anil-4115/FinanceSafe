package com.financialfraudassistant.dto;

import java.math.BigDecimal;
import java.util.List;

public record DecisionResponse(
        int score,
        String assessment,
        String decisionType,
        String summary,
        BigDecimal projectedMonthlyCost,
        Integer healthBefore,
        Integer healthAfter,
        String goalImpact,
        List<String> reasons,
        List<String> recommendations) { }