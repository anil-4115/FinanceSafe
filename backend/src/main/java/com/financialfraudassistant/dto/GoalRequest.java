package com.financialfraudassistant.dto; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate;
public record GoalRequest(@NotBlank @Size(max=255) String name,@NotNull @DecimalMin("0.01") BigDecimal targetAmount,@DecimalMin("0.0") BigDecimal currentAmount,LocalDate deadline,@DecimalMin("0.0") BigDecimal monthlyContribution){}
