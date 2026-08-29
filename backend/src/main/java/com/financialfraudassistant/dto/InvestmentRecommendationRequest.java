package com.financialfraudassistant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InvestmentRecommendationRequest(
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @NotNull @Min(1) Integer timeHorizonYears,
        @NotBlank @Size(max = 30) String riskTolerance) { }