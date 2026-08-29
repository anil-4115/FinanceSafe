package com.financialfraudassistant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DecisionRequest(
        @NotBlank @Size(max = 30) String decisionType,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Size(max = 2000) String description,
        BigDecimal monthlyIncome,
        BigDecimal monthlyExpenses,
        @DecimalMin("0.0") BigDecimal monthlyCost,
        @DecimalMin("0.0") Integer tenureMonths,
        @DecimalMin("0.0") BigDecimal interestRatePct,
        @Size(max = 30) String riskTolerance) { }