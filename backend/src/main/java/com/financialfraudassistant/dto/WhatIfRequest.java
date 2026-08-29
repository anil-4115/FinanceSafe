package com.financialfraudassistant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WhatIfRequest(
        @NotBlank @Size(max = 40) String scenario,
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @DecimalMin("-100.0") BigDecimal expensePctChange) { }