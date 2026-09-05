package com.financialfraudassistant.health;

import com.financialfraudassistant.model.Budget;
import com.financialfraudassistant.model.FinancialGoal;
import com.financialfraudassistant.model.FinancialProfile;
import com.financialfraudassistant.model.FinancialTransaction;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.BudgetRepository;
import com.financialfraudassistant.repository.FinancialGoalRepository;
import com.financialfraudassistant.repository.FinancialProfileRepository;
import com.financialfraudassistant.repository.FinancialTransactionRepository;
import com.financialfraudassistant.service.FinanceAnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HealthScoreService {

    private static final int W_SAVINGS = 20;
    private static final int W_EXPENSE = 15;
    private static final int W_EMERGENCY = 15;
    private static final int W_DEBT = 10;
    private static final int W_BUDGET = 15;
    private static final int W_GOAL = 10;
    private static final int W_INCOME = 10;
    private static final int W_DIVERSIFICATION = 5;

    private final FinancialProfileRepository profileRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final FinancialGoalRepository goalRepository;
    private final FinanceAnalyticsService analytics;

    public HealthScoreService(FinancialProfileRepository profileRepository, FinancialTransactionRepository transactionRepository,
                              BudgetRepository budgetRepository, FinancialGoalRepository goalRepository, FinanceAnalyticsService analytics) {
        this.profileRepository = profileRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.analytics = analytics;
    }

    public HealthScoreResponse evaluate(User user) {
        return evaluate(user, new HealthScenario(), null);
    }

    public HealthScoreResponse evaluate(User user, HealthScenario scenario) {
        return evaluate(user, scenario, null);
    }

    public HealthScoreResponse evaluate(User user, List<FinancialTransaction> transactions) {
        return evaluate(user, new HealthScenario(), transactions);
    }

    public HealthScoreResponse evaluate(User user, HealthScenario scenario, List<FinancialTransaction> provided) {
        List<FinancialTransaction> txns = provided != null
                ? provided : transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(user.getId());
        FinancialProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);

        BigDecimal baseIncome = profile != null && profile.getMonthlyIncome() != null
                ? profile.getMonthlyIncome() : analytics.averageMonthlyIncome(txns);
        BigDecimal baseExpense = profile != null && profile.getMonthlyFixedExpenses() != null
                ? profile.getMonthlyFixedExpenses() : analytics.averageMonthlyExpense(txns);
        BigDecimal income = baseIncome.add(nonNull(scenario.getMonthlyIncomeDelta()));
        BigDecimal expense = baseExpense.add(nonNull(scenario.getMonthlyExpenseDelta()));
        BigDecimal savings = nonNull(profile != null ? profile.getSavings() : null)
                .add(nonNull(scenario.getSavingsBoost()))
                .subtract(nonNull(scenario.getOneTimeSpend()));
        if (savings.signum() < 0) savings = BigDecimal.ZERO;
        BigDecimal debt = nonNull(profile != null ? profile.getDebt() : null)
                .add(nonNull(scenario.getExtraDebt()));

        List<HealthScoreResponse.ComponentScore> components = new ArrayList<>();
        components.add(savingsRate(income, expense));
        components.add(expenseRatio(income, expense));
        components.add(emergencyFund(savings, expense));
        components.add(debtBurden(debt, income));
        components.add(budgetDiscipline(user, txns));
        components.add(goalProgress(user, BigDecimal.ZERO));
        components.add(incomeStability(txns));
        components.add(diversification(profile));

        int totalScore = components.stream().mapToInt(component -> component.score() * component.weight() / 100).sum();

        List<String> strengths = strengths(components);
        List<String> weaknesses = weaknesses(components);
        List<String> recommendations = recommendations(components, profile, savings, expense);

        return new HealthScoreResponse(totalScore, label(totalScore), components, strengths, weaknesses, recommendations);
    }

    private HealthScoreResponse.ComponentScore savingsRate(BigDecimal income, BigDecimal expense) {
        if (income.signum() <= 0) return new HealthScoreResponse.ComponentScore("Savings rate", 0, W_SAVINGS, "No monthly income captured yet.");
        BigDecimal rate = income.subtract(expense).divide(income, 4, RoundingMode.HALF_UP);
        int score = clamp((int) Math.round(rate.doubleValue() / 0.5 * 100));
        return new HealthScoreResponse.ComponentScore("Savings rate", score, W_SAVINGS, rate.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
                + "% of income is kept as savings.");
    }

    private HealthScoreResponse.ComponentScore expenseRatio(BigDecimal income, BigDecimal expense) {
        if (income.signum() <= 0) return new HealthScoreResponse.ComponentScore("Expense ratio", 0, W_EXPENSE, "No monthly income captured yet.");
        BigDecimal ratio = expense.divide(income, 4, RoundingMode.HALF_UP);
        int score;
        if (ratio.doubleValue() <= 0.5) score = 100;
        else score = clamp((int) Math.round(100 - (ratio.doubleValue() - 0.5) * 150));
        return new HealthScoreResponse.ComponentScore("Expense ratio", score, W_EXPENSE,
                "Expenses consume " + ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "% of income.");
    }

    private HealthScoreResponse.ComponentScore emergencyFund(BigDecimal savings, BigDecimal expense) {
        if (expense.signum() <= 0) return new HealthScoreResponse.ComponentScore("Emergency fund", 80, W_EMERGENCY, "No monthly expense to cover yet.");
        BigDecimal target = expense.multiply(BigDecimal.valueOf(3));
        if (savings.signum() == 0) return new HealthScoreResponse.ComponentScore("Emergency fund", 0, W_EMERGENCY, "No savings set aside for emergencies.");
        BigDecimal covered = savings.divide(target, 4, RoundingMode.HALF_UP);
        int score = clamp((int) Math.round(covered.doubleValue() * 100));
        return new HealthScoreResponse.ComponentScore("Emergency fund", score, W_EMERGENCY,
                "Savings cover " + covered.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "% of a 3-month expense buffer.");
    }

    private HealthScoreResponse.ComponentScore debtBurden(BigDecimal debt, BigDecimal income) {
        if (debt.signum() == 0) return new HealthScoreResponse.ComponentScore("Debt burden", 100, W_DEBT, "No outstanding debt recorded.");
        if (income.signum() <= 0) return new HealthScoreResponse.ComponentScore("Debt burden", 40, W_DEBT, "Debt exists but monthly income is unknown.");
        BigDecimal annualIncome = income.multiply(BigDecimal.valueOf(12));
        BigDecimal dti = debt.divide(annualIncome, 4, RoundingMode.HALF_UP);
        int score;
        if (dti.doubleValue() <= 0.33) score = 100;
        else score = clamp((int) Math.round(100 - (dti.doubleValue() - 0.33) / 0.67 * 100));
        return new HealthScoreResponse.ComponentScore("Debt burden", score, W_DEBT,
                "Debt equals " + dti.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "% of annual income.");
    }

    private HealthScoreResponse.ComponentScore budgetDiscipline(User user, List<FinancialTransaction> txns) {
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCategory(user.getId());
        if (budgets.isEmpty()) return new HealthScoreResponse.ComponentScore("Budget discipline", 55, W_BUDGET, "No monthly budgets set - create budgets to gain control.");
        Map<String, BigDecimal> spent = currentMonthSpentByCategory(txns);
        List<Integer> scores = new ArrayList<>();
        for (Budget budget : budgets) {
            BigDecimal actual = spent.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
            BigDecimal limit = budget.getMonthlyLimit();
            int score;
            if (limit.signum() <= 0) score = 50;
            else if (actual.compareTo(limit) <= 0) score = clamp((int) Math.round(100 - actual.divide(limit, 4, RoundingMode.HALF_UP).doubleValue() * 10));
            else score = clamp((int) Math.round(100 - (actual.divide(limit, 4, RoundingMode.HALF_UP).doubleValue() - 1) * 150));
            scores.add(score);
        }
        int score = scores.stream().mapToInt(Integer::intValue).sum() / scores.size();
        return new HealthScoreResponse.ComponentScore("Budget discipline", score, W_BUDGET, budgets.size() + " budget(s) tracked against current-month spending.");
    }

    private HealthScoreResponse.ComponentScore goalProgress(User user, BigDecimal purchaseReduction) {
        List<FinancialGoal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (goals.isEmpty()) return new HealthScoreResponse.ComponentScore("Goal progress", 50, W_GOAL, "No financial goals set - set a goal to track progress.");
        return new HealthScoreResponse.ComponentScore("Goal progress", overallGoalProgressValue(user, purchaseReduction), W_GOAL,
                "Combined progress towards all " + goals.size() + " goal(s).");
    }

    private HealthScoreResponse.ComponentScore incomeStability(List<FinancialTransaction> txns) {
        Map<YearMonth, BigDecimal> monthlyIncome = monthlyIncomeMap(txns);
        if (monthlyIncome.size() < 3) return new HealthScoreResponse.ComponentScore("Income stability", 60, W_INCOME,
                monthlyIncome.isEmpty() ? "Not enough transaction history to judge stability." : "Only " + monthlyIncome.size() + " month(s) of income history so far.");
        List<BigDecimal> values = monthlyIncome.values().stream().toList();
        BigDecimal mean = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
        if (mean.signum() == 0) return new HealthScoreResponse.ComponentScore("Income stability", 0, W_INCOME, "No income recorded in recent months.");
        double variance = values.stream().mapToDouble(v -> Math.pow(v.doubleValue() - mean.doubleValue(), 2)).average().orElse(0);
        double std = Math.sqrt(variance);
        double cv = std / mean.doubleValue();
        int score = cv < 0.15 ? 100 : clamp((int) Math.round(100 - cv * 250));
        return new HealthScoreResponse.ComponentScore("Income stability", score, W_INCOME,
                "Month-to-month income varies by " + Math.round(cv * 100) + "% of the average.");
    }

    private HealthScoreResponse.ComponentScore diversification(FinancialProfile profile) {
        int categoryCount = 0;
        if (profile != null && profile.getPreferredCategories() != null) {
            categoryCount = List.of(profile.getPreferredCategories().split(",")).stream().map(String::trim).filter(item -> !item.isBlank()).toList().size();
        }
        boolean hasInvestments = profile != null && profile.getExistingInvestments() != null && profile.getExistingInvestments().signum() > 0;
        if (categoryCount >= 3) return new HealthScoreResponse.ComponentScore("Diversification", 85, W_DIVERSIFICATION, "Interests span " + categoryCount + " investment categories.");
        if (hasInvestments) return new HealthScoreResponse.ComponentScore("Diversification", 70, W_DIVERSIFICATION, "Investments recorded - consider spreading across asset classes.");
        return new HealthScoreResponse.ComponentScore("Diversification", 50, W_DIVERSIFICATION, "Add your investment categories to improve diversification insight.");
    }

    public BigDecimal overallGoalProgress(User user) {
        return overallGoalProgress(user, BigDecimal.ZERO);
    }

    public BigDecimal overallGoalProgress(User user, BigDecimal purchaseReduction) {
        List<FinancialGoal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (goals.isEmpty()) return BigDecimal.ZERO;
        BigDecimal targetTotal = goals.stream().map(FinancialGoal::getTargetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentTotal = goals.stream().map(FinancialGoal::getCurrentAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(purchaseReduction);
        if (targetTotal.signum() <= 0) return BigDecimal.ZERO;
        if (currentTotal.signum() < 0) currentTotal = BigDecimal.ZERO;
        return currentTotal.multiply(BigDecimal.valueOf(100)).divide(targetTotal, 1, RoundingMode.HALF_UP);
    }

    private int overallGoalProgressValue(User user, BigDecimal purchaseReduction) {
        return overallGoalProgress(user, purchaseReduction).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private Map<String, BigDecimal> currentMonthSpentByCategory(List<FinancialTransaction> txns) {
        YearMonth current = YearMonth.now();
        return txns.stream()
                .filter(transaction -> transaction.getTransactionType() == FinancialTransaction.Type.EXPENSE)
                .filter(transaction -> YearMonth.from(transaction.getTransactionDate()).equals(current))
                .collect(Collectors.groupingBy(FinancialTransaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, FinancialTransaction::getAmount, BigDecimal::add)));
    }

    private Map<YearMonth, BigDecimal> monthlyIncomeMap(List<FinancialTransaction> txns) {
        return txns.stream()
                .filter(transaction -> transaction.getTransactionType() == FinancialTransaction.Type.INCOME)
                .collect(Collectors.groupingBy(transaction -> YearMonth.from(transaction.getTransactionDate()),
                        Collectors.reducing(BigDecimal.ZERO, FinancialTransaction::getAmount, BigDecimal::add)));
    }

    private List<String> strengths(List<HealthScoreResponse.ComponentScore> components) {
        List<String> strengths = new ArrayList<>();
        components.stream().filter(component -> component.weight() > 0 && component.score() >= 70).forEach(component -> {
            switch (component.name()) {
                case "Savings rate" -> strengths.add("Good savings rate - a solid share of income is kept aside.");
                case "Expense ratio" -> strengths.add("Expenses are under control relative to income.");
                case "Emergency fund" -> strengths.add("Emergency buffer is well funded.");
                case "Debt burden" -> strengths.add("Debt is manageable relative to income.");
                case "Budget discipline" -> strengths.add("Healthy budget adherence.");
                case "Goal progress" -> strengths.add("Progress is being made towards goals.");
                case "Income stability" -> strengths.add("Income is stable month to month.");
                case "Diversification" -> strengths.add("Investment interests are well spread.");
                default -> { }
            }
        });
        return strengths;
    }

    private List<String> weaknesses(List<HealthScoreResponse.ComponentScore> components) {
        List<String> weaknesses = new ArrayList<>();
        components.stream().filter(component -> component.weight() > 0 && component.score() < 50).forEach(component -> {
            switch (component.name()) {
                case "Savings rate" -> weaknesses.add("Savings rate is low - little income remains after expenses.");
                case "Expense ratio" -> weaknesses.add("A large share of income goes towards expenses.");
                case "Emergency fund" -> weaknesses.add("Emergency fund is below target.");
                case "Debt burden" -> weaknesses.add("Debt is high relative to income.");
                case "Budget discipline" -> weaknesses.add("Some budgets are being exceeded.");
                case "Goal progress" -> weaknesses.add("Goals need attention - progress is limited.");
                case "Income stability" -> weaknesses.add("Income fluctuates significantly between months.");
                case "Diversification" -> weaknesses.add("Investment spread is limited.");
                default -> { }
            }
        });
        return weaknesses;
    }

    private List<String> recommendations(List<HealthScoreResponse.ComponentScore> components, FinancialProfile profile, BigDecimal savings, BigDecimal expense) {
        List<String> recommendations = new ArrayList<>();
        components.forEach(component -> {
            if (component.score() < 65) {
                switch (component.name()) {
                    case "Savings rate" -> recommendations.add("Try to keep at least 20-30% of income as savings; reduce non-essential spending.");
                    case "Expense ratio" -> recommendations.add("Lower fixed expenses or boost income so expenses stay below ~70% of income.");
                    case "Emergency fund" -> recommendations.add("Build an emergency fund covering 3-6 months of expenses. Current target: about " + money(expense.multiply(BigDecimal.valueOf(3))) + ".");
                    case "Debt burden" -> recommendations.add("Prioritise paying down high-interest debt before increasing discretionary spending.");
                    case "Budget discipline" -> recommendations.add("Review each budget where spending exceeded the limit and adjust limits or habits.");
                    case "Goal progress" -> recommendations.add("Set monthly contributions towards each goal and automate them.");
                    case "Income stability" -> recommendations.add("Build a more predictable income stream; keep an updated financial profile.");
                    case "Diversification" -> recommendations.add("Add your investment categories in your profile so the tool can guide diversification.");
                    default -> { }
                }
            }
        });
        if (profile == null || profile.getMonthlyIncome() == null) {
            recommendations.add("Complete your financial profile so scores reflect your real income and expenses.");
        }
        if (savings.signum() == 0) {
            recommendations.add("Record your current savings in your financial profile.");
        }
        return recommendations;
    }

    private String label(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 65) return "Good";
        if (score >= 50) return "Fair";
        if (score >= 35) return "Needs attention";
        return "At risk";
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
    private static BigDecimal nonNull(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value.doubleValue());
    }
}
