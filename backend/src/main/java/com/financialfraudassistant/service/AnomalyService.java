package com.financialfraudassistant.service;

import com.financialfraudassistant.model.FinancialTransaction;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FinancialTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AnomalyService {

    public record Result(int score, String level, List<String> reasons) { }

    private static final Set<String> HIGH_RISK_TERMS = Set.of(
            "crypto", "gift card", "gambling", "betting", "upi collect", "wire transfer", "investment scheme",
            "lottery", "casino", "forex", "binary");

    private final FinancialTransactionRepository transactionRepository;

    public AnomalyService(FinancialTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Result assess(User user, Integer candidateId, String merchant, BigDecimal amount, String category, LocalDate date) {
        List<FinancialTransaction> history = transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(user.getId()).stream()
                .filter(item -> item.getTransactionType() == FinancialTransaction.Type.EXPENSE)
                .filter(item -> candidateId == null || !item.getId().equals(candidateId))
                .toList();

        boolean enoughHistory = history.size() >= 3;
        int score = 5;
        List<String> reasons = new ArrayList<>();

        if (enoughHistory) {
            List<Double> logAmounts = history.stream().map(item -> Math.log(item.getAmount().doubleValue())).toList();
            double mean = logAmounts.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double variance = logAmounts.stream().mapToDouble(value -> Math.pow(value - mean, 2)).average().orElse(0);
            double std = Math.sqrt(variance);
            double candidateLog = Math.log(amount.doubleValue());
            double z = std < 1e-9 ? 0 : (candidateLog - mean) / std;
            if (z >= 2.5) { score += 30; reasons.add("Amount is " + timesFor(z) + " above what you normally pay in a single transaction."); }
            else if (z >= 2.0) { score += 24; reasons.add("Amount is unusually high compared with your normal transactions."); }
            else if (z >= 1.5) { score += 16; reasons.add("Amount is somewhat above your usual transaction size."); }

            long merchantCount = history.stream().filter(item -> item.getMerchant().equalsIgnoreCase(merchant)).count();
            BigDecimal average = history.stream().map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(history.size()), 2, java.math.RoundingMode.HALF_UP);
            if (merchantCount == 0 && amount.compareTo(average.multiply(BigDecimal.valueOf(1.5))) > 0) {
                score += 20; reasons.add("New merchant with a higher-than-average payment: " + merchant + ".");
            } else if (merchantCount == 0) {
                score += 6; reasons.add("Merchant not seen in your previous transactions: " + merchant + ".");
            }

            long categoryCount = history.stream().filter(item -> item.getCategory().equalsIgnoreCase(category)).count();
            if (categoryCount == 0) score += 10;
            else if (categoryCount <= history.size() / 5 && amount.compareTo(average) > 0) score += 8;

            long weekendHistory = history.stream().filter(item -> isWeekend(item.getTransactionDate())).count();
            boolean isWeekend = isWeekend(date);
            if (weekendHistory <= history.size() / 10 && isWeekend) { score += 6; reasons.add("Transaction on a day you rarely spend."); }

            long recentBurst = history.stream()
                    .filter(item -> !item.getTransactionDate().isBefore(date.minusDays(7)) && !item.getTransactionDate().isAfter(date))
                    .count();
            if (recentBurst >= 4) { score += 8; reasons.add("Several transactions recorded close together around this date."); }
        } else {
            score += 8;
            reasons.add("Not enough history to compare behaviour (only a few recorded expense records).");
        }

        String searchable = (merchant + " " + category).toLowerCase(Locale.ROOT);
        boolean highRiskTerm = HIGH_RISK_TERMS.stream().anyMatch(searchable::contains);
        if (highRiskTerm) { score += 20; reasons.add("Merchant or category is frequently used in financial fraud (" + merchant + ")."); }

        score = Math.min(95, score);
        String level = score < 35 ? "LOW" : score < 60 ? "MODERATE" : score < 80 ? "HIGH" : "CRITICAL";
        if (reasons.isEmpty()) reasons.add("This transaction looks consistent with your normal spending pattern.");
        return new Result(score, level, reasons);
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private static String timesFor(double z) {
        if (z >= 4) return "more than 3 times";
        if (z >= 3) return "around 3 times";
        return "much more than";
    }
}