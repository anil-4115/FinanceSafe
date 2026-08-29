package com.financialfraudassistant.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        HealthScoreResponse health,
        int fraudSafetyScore,
        List<String> fraudSafetyReasons,
        BigDecimal monthlyIncome,
        BigDecimal monthlyExpenses,
        BigDecimal savings,
        int goalProgress,
        List<MonthlySpend> monthlySpend,
        List<CategorySpend> categorySpend,
        List<AlertResponse> recentAlerts,
        List<TransactionResponse> flaggedTransactions,
        String recommendationTitle,
        String recommendationBody) {

    public record MonthlySpend(String month, BigDecimal income, BigDecimal expense) { }

    public record CategorySpend(String category, BigDecimal amount, double pct) { }
}