package com.financialfraudassistant.dto; import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record BudgetRequest(@NotBlank @Size(max=100) String category,@NotNull @DecimalMin("0.01") BigDecimal monthlyLimit){}
