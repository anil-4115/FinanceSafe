package com.financialfraudassistant.dto;
import com.financialfraudassistant.model.FinancialTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
public record TransactionResponse(Integer id, LocalDate transactionDate, String merchant, BigDecimal amount,
                                  FinancialTransaction.Type transactionType, String category,
                                  FinancialTransaction.Source source, String notes,
                                  Integer riskScore, String riskLevel, String riskReason) {
    public static TransactionResponse from(FinancialTransaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getTransactionDate(), transaction.getMerchant(),
                transaction.getAmount(), transaction.getTransactionType(), transaction.getCategory(),
                transaction.getSource(), transaction.getNotes(),
                transaction.getRiskScore(), transaction.getRiskLevel(), transaction.getRiskReason());
    }
}