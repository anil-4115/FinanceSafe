package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.AlertResponse;
import com.financialfraudassistant.dto.DashboardResponse;
import com.financialfraudassistant.dto.HealthScoreResponse;
import com.financialfraudassistant.dto.TransactionResponse;
import com.financialfraudassistant.model.Alert;
import com.financialfraudassistant.model.FinancialProfile;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.AlertRepository;
import com.financialfraudassistant.repository.FinancialProfileRepository;
import com.financialfraudassistant.repository.FinancialTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final HealthScoreService healthScoreService;
    private final FinanceAnalyticsService analytics;
    private final AlertRepository alertRepository;
    private final FinancialProfileRepository profileRepository;
    private final FinancialTransactionRepository transactionRepository;

    public DashboardService(HealthScoreService healthScoreService, FinanceAnalyticsService analytics,
                            AlertRepository alertRepository, FinancialProfileRepository profileRepository,
                            FinancialTransactionRepository transactionRepository) {
        this.healthScoreService = healthScoreService;
        this.analytics = analytics;
        this.alertRepository = alertRepository;
        this.profileRepository = profileRepository;
        this.transactionRepository = transactionRepository;
    }

    public DashboardResponse build(User user) {
        HealthScoreResponse health = healthScoreService.evaluate(user);
        List<Alert> openAlerts = alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(alert -> alert.getStatus() != Alert.Status.RESOLVED)
                .toList();
        List<AlertResponse> recentAlerts = openAlerts.stream().limit(5).map(AlertResponse::from).toList();

        FinancialProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        BigDecimal income = profile != null && profile.getMonthlyIncome() != null
                ? profile.getMonthlyIncome() : analytics.currentMonthIncome(user);
        BigDecimal expenses = profile != null && profile.getMonthlyFixedExpenses() != null
                ? profile.getMonthlyFixedExpenses() : analytics.currentMonthExpenses(user);
        BigDecimal savings = profile != null && profile.getSavings() != null
                ? profile.getSavings() : BigDecimal.ZERO;
        int goalProgress = healthScoreService.overallGoalProgress(user).setScale(0, java.math.RoundingMode.HALF_UP).intValue();

        int fraudSafetyScore = fraudSafetyScore(user);
        List<String> riskReasons = fraudRiskReasons(user);

        List<TransactionResponse> flagged = transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(user.getId()).stream()
                .filter(transaction -> transaction.getRiskScore() != null && transaction.getRiskScore() >= 50)
                .limit(5)
                .map(TransactionResponse::from)
                .toList();

        String recommendationTitle = "Your top priority today";
        String recommendationBody = health.recommendations().isEmpty()
                ? "Complete your financial profile to receive personalised recommendations."
                : health.recommendations().get(0);

        return new DashboardResponse(
                health,
                fraudSafetyScore,
                riskReasons,
                income,
                expenses,
                savings,
                goalProgress,
                analytics.monthlySeries(user, 6),
                analytics.categoryBreakdown(user),
                recentAlerts,
                flagged,
                recommendationTitle,
                recommendationBody);
    }

    private int fraudSafetyScore(User user) {
        int score = 100;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<Alert> recent = alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(alert -> alert.getCreatedAt() != null && alert.getCreatedAt().isAfter(cutoff))
                .toList();
        for (Alert alert : recent) {
            int deduction = switch (alert.getSeverity()) {
                case CRITICAL -> 16;
                case WARNING -> 7;
                case INFO -> 2;
            };
            boolean resolved = alert.getStatus() == Alert.Status.RESOLVED;
            score -= resolved ? deduction / 2 : deduction;
        }
        long risky = transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(user.getId()).stream()
                .filter(transaction -> transaction.getRiskScore() != null && transaction.getRiskScore() >= 70).count();
        score -= (int) risky * 4;
        return Math.max(0, Math.min(100, score));
    }

    private List<String> fraudRiskReasons(User user) {
        List<String> reasons = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<Alert> alerts = alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        long critical = alerts.stream().filter(alert -> alert.getCreatedAt() != null && alert.getCreatedAt().isAfter(cutoff))
                .filter(alert -> alert.getSeverity() == Alert.Severity.CRITICAL).count();
        if (critical > 0) reasons.add(critical + " critical fraud alert(s) raised in the last 30 days.");

        long openHigh = alerts.stream().filter(alert -> alert.getStatus() != Alert.Status.RESOLVED)
                .filter(alert -> alert.getSeverity() != Alert.Severity.INFO).count();
        if (openHigh > 0) reasons.add(openHigh + " open high-severity alert(s) still need your attention.");

        long risky = transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(user.getId()).stream()
                .filter(transaction -> transaction.getRiskScore() != null && transaction.getRiskScore() >= 70).count();
        if (risky > 0) reasons.add(risky + " transaction(s) were auto-flagged with high risk in your history.");

        if (reasons.isEmpty()) reasons.add("No significant fraud signals detected recently. Stay alert to unexpected messages and links.");
        return reasons;
    }
}