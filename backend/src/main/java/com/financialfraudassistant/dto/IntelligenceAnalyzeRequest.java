package com.financialfraudassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IntelligenceAnalyzeRequest(
        @NotBlank @Size(max = 10000) String content) { }
