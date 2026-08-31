package com.financialfraudassistant.service;

import com.financialfraudassistant.health.HealthScoreService;
import com.financialfraudassistant.health.HealthScenario;

import com.financialfraudassistant.dto.DecisionRequest;
import com.financialfraudassistant.dto.DecisionResponse;
import com.financialfraudassistant.model.FinancialProfile;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FinancialProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DecisionService {

    private final FinancialProfileRepository profileRepository;
    private final HealthScoreService healthScoreService;
    private final FinanceAnalyticsService analytics;
    private final ScamAnalysisService scamAnalysisService;

    public DecisionService(FinancialProfileRepository profileRepository, HealthScoreService healthScoreService,
                           FinanceAnalyticsService analytics, ScamAnalysisService scamAnalysisService) {
        this.profileRepository = profileRepository;
        this.healthScoreService = healthScoreService;
        this.analytics = analytics;
        this.scamAnalysisService = scamAnalysisService;
    }

    public DecisionResponse analyze(User user, DecisionRequest request) {
        FinancialProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        BigDecimal income = value(request.monthlyIncome(), profile != null ? profile.getMonthlyIncome() : null,
                analytics.averageMonthlyIncome(user));
        BigDecimal expense = value(request.monthlyExpenses(), profile != null ? profile.getMonthlyFixedExpenses() : null,
                analytics.averageMonthlyExpense(user));
        BigDecimal savings = profile != null && profile.getSavings() != null ? profile.getSavings() : BigDecimal.ZERO;

        return switch (request.decisionType().toUpperCase(Locale.ROOT)) {
            case "PURCHASE" -> analyzePurchase(user, request, income, expense, savings, profile);
            case "LOAN" -> analyzeLoan(user, request, income, expense, savings);
            case "INVESTMENT" -> analyzeInvestment(user, request, income, expense, profile);
            case "PAYMENT_REQUEST" -> analyzePaymentRequest(user, request, income, expense, savings);
            default -> analyzeGeneral(user, request, income, expense, savings);
        };
    }

    private DecisionResponse analyzePurchase(User user, DecisionRequest request, BigDecimal income, BigDecimal expense,
                                             BigDecimal savings, FinancialProfile profile) {
        BigDecimal amount = request.amount();
        BigDecimal surplus = income.subtract(expense);
        int healthBefore = healthScoreService.evaluate(user).score();
        HealthScenario scenario = new HealthScenario();
        scenario.setOneTimeSpend(amount);
        int healthAfter = healthScoreService.evaluate(user, scenario).score();

        int affordability;
        String affordabilityNote;
        if (amount.compareTo(savings) <= 0) { affordability = 100; affordabilityNote = "You can cover this from your existing savings."; }
        else if (amount.compareTo(savings.add(surplus)) <= 0) { affordability = 72; affordabilityNote = "You will need to dip into this month's surplus in addition to savings."; }
        else if (amount.compareTo(savings.add(surplus.multiply(BigDecimal.valueOf(6)))) <= 0) { affordability = 42; affordabilityNote = "This is a significant purchase - it would use several months of surplus."; }
        else { affordability = 15; affordabilityNote = "You do not currently have the savings to cover this comfortably."; }

        int score = clamp((int) Math.round(healthBefore * 0.6 + affordability * 0.4));
        List<String> reasons = new ArrayList<>();
        reasons.add("Financial health score: " + healthBefore + "/100.");
        reasons.add(affordabilityNote);
        reasons.add("Health impact: the score would move to about " + healthAfter + "/100 if paid from savings.");

        BigDecimal goalBefore = healthScoreService.overallGoalProgress(user);
        BigDecimal goalAfter = healthScoreService.overallGoalProgress(user, amount);
        String goalImpact = goalBefore.signum() == 0 ? "Set a goal first to see the impact on your progress."
                : "Overall goal progress would change from " + goalBefore.setScale(0, RoundingMode.HALF_UP) + "% to "
                + goalAfter.setScale(0, RoundingMode.HALF_UP) + "% if this purchase is funded from goal savings.";

        List<String> recommendations = new ArrayList<>();
        if (amount.compareTo(savings) > 0) recommendations.add("Consider waiting and saving for a few months instead of using credit.");
        recommendations.add("Before confirming, re-check the seller and the payment method; never pay an 'advance' to a stranger.");
        if (healthAfter < healthBefore) recommendations.add("Delay or reduce the purchase to keep your financial health from falling.");

        return new DecisionResponse(score, assessment(score), "PURCHASE",
                "A " + money(amount) + " purchase", null, healthBefore, healthAfter, goalImpact, reasons, recommendations);
    }

    private DecisionResponse analyzeLoan(User user, DecisionRequest request, BigDecimal income, BigDecimal expense, BigDecimal savings) {
        BigDecimal amount = request.amount();
        BigDecimal interest = request.interestRatePct() == null ? BigDecimal.valueOf(12) : request.interestRatePct();
        int tenure = request.tenureMonths() == null ? 36 : request.tenureMonths();
        BigDecimal emi = emi(amount, interest, tenure);
        BigDecimal surplus = income.subtract(expense);
        int healthBefore = healthScoreService.evaluate(user).score();

        HealthScenario scenario = new HealthScenario();
        scenario.setMonthlyExpenseDelta(emi);
        scenario.setExtraDebt(amount);
        int healthAfter = healthScoreService.evaluate(user, scenario).score();

        BigDecimal dti = income.signum() <= 0 ? BigDecimal.valueOf(0.5) : emi.divide(income, 4, RoundingMode.HALF_UP);
        int dtiScore;
        if (dti.doubleValue() <= 0.3) dtiScore = 100;
        else if (dti.doubleValue() <= 0.5) dtiScore = 60;
        else dtiScore = 20;

        int score = clamp((int) Math.round(healthBefore * 0.5 + dtiScore * 0.5));
        List<String> reasons = new ArrayList<>();
        reasons.add("Estimated monthly instalment: " + money(emi) + " over " + tenure + " months at " + interest.setScale(0, RoundingMode.HALF_UP) + "% interest.");
        reasons.add("The instalment would use " + dti.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "% of your monthly income (a healthy loan keeps this under ~30%).");
        reasons.add("Your financial health would move from " + healthBefore + "/100 to about " + healthAfter + "/100.");
        if (emi.compareTo(surplus) > 0) reasons.add("The instalment exceeds your current monthly surplus — this loan would pressure your cash flow.");

        List<String> recommendations = new ArrayList<>();
        if (dti.doubleValue() > 0.3) recommendations.add("Look for a longer tenure or lower amount so the instalment stays under ~30% of income.");
        recommendations.add("Compare at least three lenders and read the full effective interest rate (APR) before signing.");
        recommendations.add("Never pay a 'processing fee' before a loan is disbursed — that is a common scam pattern.");

        return new DecisionResponse(score, assessment(score), "LOAN",
                "A " + money(amount) + " loan", emi, healthBefore, healthAfter, null, reasons, recommendations);
    }

    private DecisionResponse analyzeInvestment(User user, DecisionRequest request, BigDecimal income, BigDecimal expense,
                                               FinancialProfile profile) {
        BigDecimal amount = request.amount();
        int healthBefore = healthScoreService.evaluate(user).score();
        boolean hasTolerance = profile != null && profile.getRiskTolerance() != null && request.riskTolerance() != null;
        boolean match = hasTolerance && profile.getRiskTolerance().equalsIgnoreCase(request.riskTolerance());
        String description = request.description() == null ? "" : request.description().toLowerCase(Locale.ROOT);
        boolean scamBait = List.of("guaranteed", "double your money", "triple", "no risk", "get rich", "crypto",
                "forex", "lottery", "register now").stream().anyMatch(description::contains);

        int score = clamp((int) Math.round(healthBefore * 0.6 + (match ? 30 : 12) + (scamBait ? -40 : 0)));
        List<String> reasons = new ArrayList<>();
        reasons.add("Your financial health score is " + healthBefore + "/100.");
        reasons.add(match ? "This investment matches the risk tolerance recorded in your profile."
                : "This does not clearly match your recorded risk tolerance (" + (profile != null ? profile.getRiskTolerance() : "not set") + ").");
        if (scamBait) reasons.add("The description uses phrases typical of investment fraud (guaranteed returns, get-rich-quick).");
        reasons.add("Investments are volatile; never invest money you may need within the next few years.");

        List<String> recommendations = new ArrayList<>();
        if (scamBait) recommendations.add("Treat 'guaranteed high returns' as a red flag — legitimate investments carry risk.");
        recommendations.add("Diversify rather than putting everything into one product, and check SEBI-registered sources.");
        recommendations.add("Keep an emergency fund separate from any investment.");

        return new DecisionResponse(Math.max(5, score), assessment(score), "INVESTMENT",
                "An investment of " + money(amount), null, null, null, null, reasons, recommendations);
    }

    private DecisionResponse analyzePaymentRequest(User user, DecisionRequest request, BigDecimal income, BigDecimal expense,
                                                   BigDecimal savings) {
        BigDecimal amount = request.amount();
        List<String> reasons = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int fraudRisk = 0;
        if (request.description() != null && !request.description().isBlank()) {
            fraudRisk = scamAnalysisService.analyze(user, request.description(), "TEXT").getRiskScore();
            reasons.add("Message analysis scored the related content at " + fraudRisk + "/100 risk.");
        }
        boolean aboveNormal = amount.compareTo(income.multiply(BigDecimal.valueOf(0.5))) > 0;
        if (aboveNormal) reasons.add("Requested amount is more than half your monthly income — verify before paying.");
        int credit = fraudRisk >= 60 ? 0 : fraudRisk >= 35 ? 20 : 35;
        if (amount.compareTo(savings) > 0) credit -= 10;
        int score = clamp(100 - (fraudRisk > 0 ? fraudRisk : 15) + credit - (aboveNormal ? 8 : 0));
        recommendations.add("Open your UPI/banking app manually and check the request there; never approve from a message link.");
        if (fraudRisk >= 40) recommendations.add("Do not approve the collect/request. Report the sender and block if unknown.");
        recommendations.add("An official provider will never ask you to share an OTP, UPI PIN or password.");
        return new DecisionResponse(Math.max(5, score), assessment(score), "PAYMENT_REQUEST",
                "A payment request of " + money(amount), null, null, null, null, reasons, recommendations);
    }

    private DecisionResponse analyzeGeneral(User user, DecisionRequest request, BigDecimal income, BigDecimal expense, BigDecimal savings) {
        int health = healthScoreService.evaluate(user).score();
        List<String> reasons = new ArrayList<>();
        reasons.add("Your current financial health is " + health + "/100.");
        reasons.add("Think about whether this decision protects or harms your savings, budget and goals.");
        return new DecisionResponse(clamp(health), assessment(health), request.decisionType().toUpperCase(Locale.ROOT),
                "Decision of " + money(request.amount()), null, health, health, null, reasons,
                List.of("Re-run this check with a specific type: PURCHASE, LOAN, INVESTMENT or PAYMENT_REQUEST."));
    }

    private String assessment(int score) {
        if (score >= 75) return "SAFE";
        if (score >= 55) return "CAUTION";
        return "RISKY";
    }

    private static BigDecimal emi(BigDecimal principal, BigDecimal annualRatePct, int months) {
        if (months <= 0) return principal;
        BigDecimal monthlyRate = annualRatePct.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        if (monthlyRate.signum() == 0) return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months);
        return principal.multiply(monthlyRate).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal value(BigDecimal requested, BigDecimal profileValue, BigDecimal computed) {
        if (requested != null) return requested;
        if (profileValue != null) return profileValue;
        return computed;
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
    private static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value.doubleValue());
    }
}