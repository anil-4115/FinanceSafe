package com.financialfraudassistant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InvestmentSimulationRequest(
        @NotNull @DecimalMin("0.0") BigDecimal initialInvestment,
        @NotNull @DecimalMin("0.0") BigDecimal monthlyContribution,
        @NotNull @Min(1) Integer years,
        @NotNull @DecimalMin("0.0") BigDecimal annualReturnPct) { }