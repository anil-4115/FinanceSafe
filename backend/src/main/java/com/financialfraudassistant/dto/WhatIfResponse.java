package com.financialfraudassistant.dto;

import java.math.BigDecimal;
import java.util.List;

public record WhatIfResponse(
        int healthBefore,
        int healthAfter,
        BigDecimal savingsBefore,
        BigDecimal savingsAfter,
        BigDecimal goalProgressBefore,
        BigDecimal goalProgressAfter,
        List<String> explanations,
        List<String> recommendations) { }