package com.financialfraudassistant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FinancialProfileRequest(
        @Size(max = 50) String ageRange, @Size(max = 100) String employmentType,
        @DecimalMin("0.0") BigDecimal monthlyIncome, @DecimalMin("0.0") BigDecimal monthlyFixedExpenses,
        @DecimalMin("0.0") BigDecimal savings, @DecimalMin("0.0") BigDecimal existingInvestments,
        @DecimalMin("0.0") BigDecimal debt, @Size(max = 50) String riskTolerance,
        @Size(max = 50) String investmentExperience, @Size(max = 2000) String preferredCategories
) { }
