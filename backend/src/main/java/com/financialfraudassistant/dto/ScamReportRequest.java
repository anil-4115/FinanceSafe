package com.financialfraudassistant.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
public record ScamReportRequest(@NotBlank @Size(max = 50) String channel, @NotBlank @Size(max = 4000) String description,
                                @DecimalMin("0.0") BigDecimal amountAtRisk) { }
