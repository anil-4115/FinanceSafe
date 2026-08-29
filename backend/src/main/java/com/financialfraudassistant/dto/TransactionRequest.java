package com.financialfraudassistant.dto;
import com.financialfraudassistant.model.FinancialTransaction.Type;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
public record TransactionRequest(
        @NotNull LocalDate transactionDate, @NotBlank @Size(max = 255) String merchant,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount, @NotNull Type transactionType,
        @NotBlank @Size(max = 100) String category, @Size(max = 2000) String notes
) { }
