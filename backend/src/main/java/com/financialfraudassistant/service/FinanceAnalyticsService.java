package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.DashboardResponse.MonthlySpend;
import com.financialfraudassistant.dto.DashboardResponse.CategorySpend;
import com.financialfraudassistant.model.FinancialTransaction;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FinancialTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinanceAnalyticsService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM");
    private final FinancialTransactionRepository transactionRepository;

    public FinanceAnalyticsService(FinancialTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<FinancialTransaction> transactions(User user) {
        return transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(user.getId());
    }

    public BigDecimal currentMonthIncome(User user) {
        return currentMonthIncome(transactions(user));
    }

    public BigDecimal currentMonthExpenses(User user) {
        return currentMonthExpenses(transactions(user));
    }

    public BigDecimal averageMonthlyExpense(User user) {
        return averageMonthlyExpense(transactions(user));
    }

    public BigDecimal averageMonthlyIncome(User user) {
        return averageMonthlyIncome(transactions(user));
    }

    public List<MonthlySpend> monthlySeries(User user, int months) {
        return monthlySeries(transactions(user), months);
    }

    public List<CategorySpend> categoryBreakdown(User user) {
        return categoryBreakdown(transactions(user));
    }

    public LocalDate oldestTransactionDate(User user) {
        return transactions(user).stream().map(FinancialTransaction::getTransactionDate).min(LocalDate::compareTo).orElse(null);
    }

    public BigDecimal currentMonthIncome(List<FinancialTransaction> txns) {
        YearMonth current = YearMonth.now();
        return txns.stream()
                .filter(transaction -> transaction.getTransactionType() == FinancialTransaction.Type.INCOME)
                .filter(transaction -> YearMonth.from(transaction.getTransactionDate()).equals(current))
                .map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal currentMonthExpenses(List<FinancialTransaction> txns) {
        YearMonth current = YearMonth.now();
        return txns.stream()
                .filter(transaction -> transaction.getTransactionType() == FinancialTransaction.Type.EXPENSE)
                .filter(transaction -> YearMonth.from(transaction.getTransactionDate()).equals(current))
                .map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal averageMonthlyExpense(List<FinancialTransaction> txns) {
        Map<YearMonth, BigDecimal> byMonth = monthlyExpenseMap(txns);
        if (byMonth.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = byMonth.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(byMonth.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal averageMonthlyIncome(List<FinancialTransaction> txns) {
        Map<YearMonth, BigDecimal> byMonth = monthlyIncomeMap(txns);
        if (byMonth.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = byMonth.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(byMonth.size()), 2, RoundingMode.HALF_UP);
    }

    public List<MonthlySpend> monthlySeries(List<FinancialTransaction> txns, int months) {
        Map<YearMonth, BigDecimal> expenses = monthlyExpenseMap(txns);
        Map<YearMonth, BigDecimal> incomes = monthlyIncomeMap(txns);
        List<MonthlySpend> series = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(months - 1L);
        for (int i = 0; i < months; i++) {
            YearMonth month = start.plusMonths(i);
            series.add(new MonthlySpend(month.format(MONTH_LABEL),
                    incomes.getOrDefault(month, BigDecimal.ZERO),
                    expenses.getOrDefault(month, BigDecimal.ZERO)));
        }
        return series;
    }

    public List<CategorySpend> categoryBreakdown(List<FinancialTransaction> txns) {
        YearMonth current = YearMonth.now();
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (FinancialTransaction transaction : txns) {
            if (transaction.getTransactionType() == FinancialTransaction.Type.EXPENSE
                    && YearMonth.from(transaction.getTransactionDate()).equals(current)) {
                byCategory.merge(transaction.getCategory(), transaction.getAmount(), BigDecimal::add);
            }
        }
        BigDecimal total = byCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CategorySpend> result = new ArrayList<>();
        byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEach(entry -> {
                    double pct = total.signum() == 0 ? 0 : entry.getValue().doubleValue() / total.doubleValue() * 100;
                    result.add(new CategorySpend(entry.getKey(), entry.getValue(), Math.round(pct * 10.0) / 10.0));
                });
        return result;
    }

    private Map<YearMonth, BigDecimal> monthlyExpenseMap(List<FinancialTransaction> txns) {
        Map<YearMonth, BigDecimal> byMonth = new LinkedHashMap<>();
        for (FinancialTransaction transaction : txns) {
            if (transaction.getTransactionType() == FinancialTransaction.Type.EXPENSE) {
                byMonth.merge(YearMonth.from(transaction.getTransactionDate()), transaction.getAmount(), BigDecimal::add);
            }
        }
        return byMonth;
    }

    private Map<YearMonth, BigDecimal> monthlyIncomeMap(List<FinancialTransaction> txns) {
        Map<YearMonth, BigDecimal> byMonth = new LinkedHashMap<>();
        for (FinancialTransaction transaction : txns) {
            if (transaction.getTransactionType() == FinancialTransaction.Type.INCOME) {
                byMonth.merge(YearMonth.from(transaction.getTransactionDate()), transaction.getAmount(), BigDecimal::add);
            }
        }
        return byMonth;
    }
}