package com.financialfraudassistant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRiskRequest(
        @NotBlank @Size(max = 255) String merchant,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(max = 100) String category,
        @NotNull LocalDate transactionDate) { }