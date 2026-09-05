package com.financialfraudassistant.service;

import com.financialfraudassistant.health.HealthScoreService;
import com.financialfraudassistant.health.HealthScenario;

import com.financialfraudassistant.dto.WhatIfRequest;
import com.financialfraudassistant.dto.WhatIfResponse;
import com.financialfraudassistant.model.DecisionAnalysis;
import com.financialfraudassistant.model.FinancialProfile;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.DecisionAnalysisRepository;
import com.financialfraudassistant.repository.FinancialProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class WhatIfService {

    private final HealthScoreService healthScoreService;
    private final FinanceAnalyticsService analytics;
    private final FinancialProfileRepository profileRepository;
    private final DecisionAnalysisRepository decisionAnalysisRepository;

    public WhatIfService(HealthScoreService healthScoreService, FinanceAnalyticsService analytics,
                         FinancialProfileRepository profileRepository, DecisionAnalysisRepository decisionAnalysisRepository) {
        this.healthScoreService = healthScoreService;
        this.analytics = analytics;
        this.profileRepository = profileRepository;
        this.decisionAnalysisRepository = decisionAnalysisRepository;
    }

    public WhatIfResponse simulate(User user, WhatIfRequest request) {
        FinancialProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        BigDecimal savings = profile != null && profile.getSavings() != null ? profile.getSavings() : BigDecimal.ZERO;

        int healthBefore = healthScoreService.evaluate(user).score();
        HealthScenario scenario = new HealthScenario();
        List<String> explanations = new ArrayList<>();
        BigDecimal savingsAfter = savings;

        switch (request.scenario().toUpperCase(Locale.ROOT)) {
            case "INCREASE_SAVINGS", "MONTHLY_INVESTMENT" -> {
                scenario.setSavingsBoost(request.amount());
                savingsAfter = savings.add(request.amount());
                explanations.add("Adding " + money(request.amount()) + " to savings every month improves the savings rate and emergency buffer.");
            }
            case "DECREASE_SPENDING" -> {
                scenario.setMonthlyExpenseDelta(request.amount().negate());
                explanations.add("Cutting " + money(request.amount()) + " from monthly expenses improves the expense ratio and savings rate.");
            }
            case "EXPENSE_INCREASE" -> {
                BigDecimal delta = request.amount() != null && request.amount().signum() > 0
                        ? request.amount()
                        : analytics.averageMonthlyExpense(user).multiply(request.expensePctChange() == null
                                ? BigDecimal.ONE.movePointLeft(2)
                                : request.expensePctChange().movePointLeft(2));
                scenario.setMonthlyExpenseDelta(delta);
                explanations.add("Raising monthly expenses by " + money(delta.abs()) + " weakens the expense ratio and savings rate.");
            }
            case "ONE_TIME_PURCHASE" -> {
                scenario.setOneTimeSpend(request.amount());
                savingsAfter = savings.subtract(request.amount());
                if (savingsAfter.signum() < 0) savingsAfter = BigDecimal.ZERO;
                explanations.add("A one-time purchase of " + money(request.amount()) + " reduces your savings and can delay goals.");
            }
            case "LOAN" -> {
                BigDecimal emi = monthlyEmi(request.amount());
                scenario.setMonthlyExpenseDelta(emi);
                scenario.setExtraDebt(request.amount());
                explanations.add("Taking a loan of " + money(request.amount()) + " adds roughly " + money(emi) + " in monthly repayments.");
            }
            default -> explanations.add("Scenario not recognised - score shown for reference only.");
        }

        int healthAfter = healthScoreService.evaluate(user, scenario).score();
        BigDecimal goalBefore = healthScoreService.overallGoalProgress(user);
        BigDecimal goalAfter = request.scenario().equalsIgnoreCase("ONE_TIME_PURCHASE")
                ? healthScoreService.overallGoalProgress(user, request.amount())
                : goalBefore;

        List<String> recommendations = new ArrayList<>();
        if (healthAfter < healthBefore) recommendations.add("Consider postponing or reducing this change to protect your financial health.");
        if (healthAfter > healthBefore) recommendations.add("This change improves your health score - try to make it a habit.");

        WhatIfResponse response = new WhatIfResponse(healthBefore, healthAfter, savings, savingsAfter,
                goalBefore.setScale(1, java.math.RoundingMode.HALF_UP),
                goalAfter.setScale(1, java.math.RoundingMode.HALF_UP),
                explanations, recommendations);
        persistAnalysis(user, request, response);
        return response;
    }

    private void persistAnalysis(User user, WhatIfRequest request, WhatIfResponse response) {
        String goalImpact = "Goal progress: " + response.goalProgressBefore() + "% before, " + response.goalProgressAfter() + "% after.";
        int safety = Math.max(0, Math.min(100, response.healthAfter()));
        decisionAnalysisRepository.save(new DecisionAnalysis(user,
                "WHAT_IF_" + request.scenario().toUpperCase(Locale.ROOT),
                request.amount(),
                request.scenario(),
                safety,
                assessment(safety),
                response.healthBefore(),
                response.healthAfter(),
                goalImpact,
                String.join("\n", response.explanations()),
                String.join("\n", response.recommendations())));
    }

    private static String assessment(int score) {
        if (score >= 75) return "SAFE";
        if (score >= 55) return "CAUTION";
        return "RISKY";
    }

    private static BigDecimal monthlyEmi(BigDecimal principal) {
        BigDecimal rate = BigDecimal.valueOf(12).divide(BigDecimal.valueOf(1200), 10, java.math.RoundingMode.HALF_UP);
        int months = 36;
        BigDecimal factor = BigDecimal.ONE.add(rate).pow(months);
        return principal.multiply(rate).multiply(factor).divide(factor.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value.doubleValue());
    }
}