package com.financialfraudassistant.service;

import com.financialfraudassistant.health.HealthScoreService;

import com.financialfraudassistant.dto.AssistantRequest;
import com.financialfraudassistant.dto.AssistantResponse;
import com.financialfraudassistant.dto.DashboardResponse.CategorySpend;
import com.financialfraudassistant.dto.InvestmentRecommendationRequest;
import com.financialfraudassistant.dto.InvestmentRecommendationResponse;
import com.financialfraudassistant.dto.MarketDetailResponse;
import com.financialfraudassistant.dto.MarketSearchResult;
import com.financialfraudassistant.model.Alert;
import com.financialfraudassistant.model.Budget;
import com.financialfraudassistant.model.ChatConversation;
import com.financialfraudassistant.model.ChatMessage;
import com.financialfraudassistant.model.FinancialGoal;
import com.financialfraudassistant.model.FinancialProfile;
import com.financialfraudassistant.model.FinancialTransaction;
import com.financialfraudassistant.model.FraudAnalysis;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.AlertRepository;
import com.financialfraudassistant.repository.BudgetRepository;
import com.financialfraudassistant.repository.ChatConversationRepository;
import com.financialfraudassistant.repository.ChatMessageRepository;
import com.financialfraudassistant.repository.FinancialGoalRepository;
import com.financialfraudassistant.repository.FinancialProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssistantService {

    private enum Intent { GREETING, SCAM_ANALYZE, HEALTH, GOAL, SPENDING, RISK, BUDGET, WHAT_IF, INVEST, LEARN, MARKET, FALLBACK }

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final FinanceAnalyticsService analytics;
    private final HealthScoreService healthScoreService;
    private final ScamAnalysisService scamAnalysisService;
    private final WhatIfService whatIfService;
    private final BudgetRepository budgetRepository;
    private final AlertRepository alertRepository;
    private final FinancialGoalRepository goalRepository;
    private final FinancialProfileRepository profileRepository;
    private final EducationService educationService;
    private final InvestmentRecommendationService investmentRecommendationService;
    private final MarketService marketService;

    public AssistantService(ChatConversationRepository conversationRepository, ChatMessageRepository messageRepository,
                            FinanceAnalyticsService analytics, HealthScoreService healthScoreService,
                            ScamAnalysisService scamAnalysisService, WhatIfService whatIfService,
                            BudgetRepository budgetRepository, AlertRepository alertRepository,
                            FinancialGoalRepository goalRepository, FinancialProfileRepository profileRepository,
                            EducationService educationService, InvestmentRecommendationService investmentRecommendationService,
                            MarketService marketService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.analytics = analytics;
        this.healthScoreService = healthScoreService;
        this.scamAnalysisService = scamAnalysisService;
        this.whatIfService = whatIfService;
        this.budgetRepository = budgetRepository;
        this.alertRepository = alertRepository;
        this.goalRepository = goalRepository;
        this.profileRepository = profileRepository;
        this.educationService = educationService;
        this.investmentRecommendationService = investmentRecommendationService;
        this.marketService = marketService;
    }

    public AssistantResponse handle(User user, AssistantRequest request) {
        String message = request.message() == null ? "" : request.message().trim();
        if (message.isBlank()) {
            return reply(user, message, "greeting", greeting(user));
        }
        Intent intent = classify(message);
        String body = switch (intent) {
            case GREETING -> greeting(user);
            case HEALTH -> health(user);
            case GOAL -> goal(user);
            case SPENDING -> spending(user);
            case RISK -> risk(user);
            case BUDGET -> budget(user);
            case WHAT_IF -> whatIf(user, message);
            case INVEST -> invest(user, message);
            case LEARN -> learn(user);
            case MARKET -> market(message);
            case SCAM_ANALYZE -> scam(user, message);
            case FALLBACK -> fallback(user, message);
        };
        return reply(user, message, intent.name().toLowerCase(Locale.ROOT), body);
    }

    public List<com.financialfraudassistant.dto.ChatMessageDto> history(User user) {
        Optional<ChatConversation> conversation = conversationRepository.findFirstByUserIdOrderByUpdatedAtDesc(user.getId());
        if (conversation.isEmpty()) return List.of();
        return messageRepository.findByConversationIdOrderById(conversation.get().getId()).stream()
                .map(message -> new com.financialfraudassistant.dto.ChatMessageDto(message.getId(),
                        message.getSender() == ChatMessage.Sender.ASSISTANT ? "assistant" : "user",
                        message.getContent(), message.getCreatedAt()))
                .toList();
    }

    private AssistantResponse reply(User user, String userMessage, String intent, String body) {
        ChatConversation conversation = conversationRepository.findFirstByUserIdOrderByUpdatedAtDesc(user.getId())
                .orElseGet(() -> conversationRepository.save(new ChatConversation(user, "FinanceSafe conversation")));
        messageRepository.save(new ChatMessage(conversation, ChatMessage.Sender.USER,
                userMessage.isBlank() ? "Can you help me?" : userMessage));
        messageRepository.save(new ChatMessage(conversation, ChatMessage.Sender.ASSISTANT, body));
        return new AssistantResponse(intent, body, SUGGESTED);
    }

    private static final List<String> SUGGESTED = List.of(
            "Is this SMS a scam? Your Aadhaar is blocked, share OTP",
            "How is my financial health?",
            "Where am I overspending?",
            "What if I save Rs 5,000 more each month?",
            "How is my budget doing this month?",
            "Should I invest my savings?");

    private Intent classify(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.matches(".*(hi|hello|hey|namaste|help me).*")) return Intent.GREETING;
        if (lower.contains("scam") || lower.contains("fraud") || lower.contains("phishing") || lower.contains("otp")
                || lower.contains("kyc") || lower.contains("is this") || lower.contains("message") || lower.contains("sms")
                || lower.contains("urgent") || lower.contains("lottery") || lower.contains("bank") || lower.contains("aadhaar")
                || lower.contains("blocked") || lower.contains("verify")) return Intent.SCAM_ANALYZE;
        if (lower.contains("health") || lower.contains("score") || lower.contains("how am i doing")) return Intent.HEALTH;
        if (lower.contains("goal") || lower.contains("achieve") || lower.contains("save for") || lower.contains("on track")
                || lower.contains("reach")) return Intent.GOAL;
        if (lower.contains("where") || lower.contains("spend") || lower.contains("overspend") || lower.contains("categories")
                || lower.contains("too much") || lower.contains("money going")) return Intent.SPENDING;
        if (lower.contains("risk") || lower.contains("flagged") || lower.contains("suspicious") || lower.contains("why did")
                || lower.contains("alerts")) return Intent.RISK;
        if (lower.contains("budget") || lower.contains("over budget") || lower.contains("limit")) return Intent.BUDGET;
        if (lower.contains("what if") || lower.contains("save more") || lower.contains("increase my savings")
                || lower.contains("reduce spending") || lower.contains("afford")) return Intent.WHAT_IF;
        if (lower.contains("invest") || lower.contains("sip") || lower.contains("fd") || lower.contains("fund")
                || lower.contains("return") || lower.contains("scheme") || lower.contains("stock")) return Intent.INVEST;
        if (lower.contains("learn") || lower.contains("lesson") || lower.contains("module") || lower.contains("quiz")
                || lower.contains("education") || lower.contains("teaching") || lower.contains("compound") || lower.contains("upi")) return Intent.LEARN;
        if (lower.contains("market") || lower.contains("share price") || lower.contains("nifty") || lower.contains("stock price")
                || lower.contains("sensex") || lower.contains("gold price") || lower.contains("today's market")) return Intent.MARKET;
        return Intent.FALLBACK;
    }

    private String greeting(User user) {
        String timeOfDay = LocalTime.now().isBefore(LocalTime.NOON) ? "Good morning" :
                LocalTime.now().isBefore(LocalTime.of(17, 0)) ? "Good afternoon" : "Good evening";
        String name = user.getFullName() == null ? "" : " " + user.getFullName().split(" ")[0];
        var score = healthScoreService.evaluate(user);
        List<Alert> alerts = alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        String firstName = name.isBlank() ? "" : "," + name;
        return timeOfDay + firstName + "! Your financial health right now is " + score.score() + "/100 (" + score.label().toLowerCase(Locale.ROOT) + ")." +
                "\n• " + alerts.size() + " alert(s) on your account" +
                "\n• " + spendingContext(user).trim() +
                "\nAsk me about your health, spending, goals, budgets, or paste a suspicious SMS to scan it.";
    }

    private String spendingContext(User user) {
        BigDecimal current = analytics.currentMonthExpenses(user);
        BigDecimal avg = analytics.averageMonthlyExpense(user);
        if (current.signum() == 0) return "You have no spending recorded yet this month.";
        String comparison = avg.signum() == 0 ? "" : (current.compareTo(avg) > 0 ? " ~" + pct(current.subtract(avg), avg) + " above your monthly average" : " ~" + pct(avg.subtract(current), avg) + " below your monthly average");
        return "You have spent " + money(current) + " so far this month" + comparison + ".";
    }

    private String health(User user) {
        var result = healthScoreService.evaluate(user);
        StringBuilder response = new StringBuilder("Your financial health score is " + result.score() + "/100 (" + result.label() + ").\n");
        if (result.components() != null) {
            result.components().stream()
                    .sorted(Comparator.comparingInt(c -> c.score()))
                    .limit(2)
                    .forEach(c -> response.append("• ").append(c.name()).append(": ").append(c.score()).append("/100\n"));
        }
        if (result.weaknesses() != null && !result.weaknesses().isEmpty()) {
            response.append("Weaknesses:\n- ").append(String.join("\n- ", result.weaknesses()));
        }
        if (result.recommendations() != null && !result.recommendations().isEmpty()) {
            response.append("\nRecommendation: ").append(result.recommendations().get(0));
        }
        return response.toString();
    }

    private String goal(User user) {
        List<FinancialGoal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (goals.isEmpty()) return "You have no savings goals yet. Add goals on the Goals page and I can track your progress toward them.";
        StringBuilder response = new StringBuilder("You have " + goals.size() + " goal(s):\n");
        goals.forEach(goal -> {
            double pct = goal.getTargetAmount().signum() == 0 ? 0
                    : goal.getCurrentAmount().doubleValue() / goal.getTargetAmount().doubleValue() * 100;
            response.append("• ").append(goal.getName()).append(": ").append(money(goal.getCurrentAmount()))
                    .append(" / ").append(money(goal.getTargetAmount()))
                    .append(" (").append(Math.round(pct)).append("%")
                    .append(goal.getStatus() == FinancialGoal.Status.COMPLETED ? ", completed" : "")
                    .append(")\n");
        });
        FinancialGoal nearest = goals.stream()
                .filter(g -> g.getStatus() != FinancialGoal.Status.COMPLETED)
                .max(Comparator.comparing(FinancialGoal::getMonthlyContribution))
                .orElse(null);
        if (nearest != null) {
            response.append("Keep contributing ").append(money(nearest.getMonthlyContribution()))
                    .append("/month to ").append(nearest.getName()).append(" to stay on track.");
        }
        return response.toString();
    }

    private String spending(User user) {
        List<CategorySpend> breakdown = analytics.categoryBreakdown(user);
        if (breakdown.isEmpty()) return "You don't have transactions this month yet. Add a few in the Spending page and I can analyse where your money goes.";
        BigDecimal current = analytics.currentMonthExpenses(user);
        BigDecimal avg = analytics.averageMonthlyExpense(user);
        BigDecimal income = analytics.currentMonthIncome(user);
        StringBuilder response = new StringBuilder("This month you have spent " + money(current) + " on expenses.\n");
        if (income.signum() > 0) {
            response.append("• Income this month: " + money(income) + "\n");
            response.append("• Savings rate: " + pct(income.subtract(current), income) + "\n");
        }
        if (avg.signum() > 0) {
            String direction = current.compareTo(avg) > 0 ? "higher than" : "lower than";
            response.append("• This is " + direction + " your monthly average of " + money(avg) + "\n");
        }
        response.append("Top categories:\n");
        breakdown.stream().limit(3)
                .forEach(entry -> response.append("- ").append(entry.category()).append(": ").append(money(entry.amount())).append(" (").append(entry.pct()).append("%)\n"));
        return response.toString();
    }

    private String risk(User user) {
        List<FinancialTransaction> flagged = analytics.transactions(user).stream()
                .filter(item -> item.getRiskScore() != null && item.getRiskScore() >= 50).toList();
        List<Alert> alerts = alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        StringBuilder response = new StringBuilder("You currently have " + flagged.size() + " flagged transaction(s) and " + alerts.size() + " total alert(s).\n");
        if (!flagged.isEmpty()) {
            response.append("Highest-risk transactions:\n");
            flagged.stream().sorted(Comparator.comparing(t -> t.getRiskScore() == null ? 0 : -t.getRiskScore())).limit(3)
                    .forEach(t -> response.append("- ").append(t.getMerchant()).append(" on ").append(t.getTransactionDate())
                            .append(": risk ").append(t.getRiskScore()).append("/100\n"));
        }
        List<Alert> unresolved = alerts.stream().filter(a -> a.getStatus() != Alert.Status.RESOLVED).toList();
        if (!unresolved.isEmpty()) {
            response.append("Unresolved alerts:\n");
            unresolved.stream().limit(3).forEach(a -> response.append("- ").append(a.getTitle()).append("\n"));
            response.append(resolveHint());
        }
        if (flagged.isEmpty() && unresolved.isEmpty()) {
            response.append("No active risks. Keep reviewing your Scam Scanner history and stay alert - scammers target anyone.");
        }
        return response.toString();
    }

    private String resolveHint() {
        return "Review these on the Alerts page - resolve ones you have handled.\n";
    }

    private String budget(User user) {
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCategory(user.getId());
        if (budgets.isEmpty()) return "You haven't set any budgets yet - set limits by category on the Budget page and I will compare your real spending against them.";
        Map<String, BigDecimal> spentByCategory = analytics.categoryBreakdown(user).stream()
                .collect(Collectors.toMap(c -> c.category().toLowerCase(Locale.ROOT), c -> c.amount(), BigDecimal::add));
        StringBuilder response = new StringBuilder("Budget vs actual spending this month:\n");
        budgets.forEach(budget -> {
            BigDecimal spent = spentByCategory.getOrDefault(budget.getCategory().toLowerCase(Locale.ROOT), BigDecimal.ZERO);
            double usedPct = budget.getMonthlyLimit().signum() == 0 ? 0 : spent.doubleValue() / budget.getMonthlyLimit().doubleValue() * 100;
            String status = spent.compareTo(budget.getMonthlyLimit()) > 0 ? "OVER BUDGET"
                    : usedPct >= 80 ? "nearing limit" : "on track";
            response.append("• ").append(budget.getCategory()).append(": ").append(money(spent))
                    .append(" / ").append(money(budget.getMonthlyLimit()))
                    .append(" (").append(Math.round(usedPct)).append("%, ").append(status).append(")\n");
        });
        boolean over = budgets.stream().anyMatch(budget -> spentByCategory.getOrDefault(budget.getCategory().toLowerCase(Locale.ROOT), BigDecimal.ZERO)
                .compareTo(budget.getMonthlyLimit()) > 0);
        if (over) response.append("You are over budget in at least one category - trim spending there or raise the limit deliberately.");
        else response.append("All budgets are within limits. Good discipline!");
        return response.toString();
    }

    private String whatIf(User user, String message) {
        if (message.matches(".*\\d{2,}.*")) {
            BigDecimal amount = firstNumber(message);
            String type = message.contains("save") || message.contains("invest") ? "INCREASE_SAVINGS"
                    : message.contains("spend") ? "DECREASE_SPENDING" : "ONE_TIME_PURCHASE";
            var result = whatIfService.simulate(user, new com.financialfraudassistant.dto.WhatIfRequest(type, amount, null));
            return "Simulation for " + money(amount) + ":\n• Health score: " + result.healthBefore() + " → " + result.healthAfter() +
                    "\n• Savings: " + money(result.savingsBefore()) + " → " + money(result.savingsAfter()) +
                    "\n" + String.join("\n", result.explanations());
        }
        return "Tell me an amount and what you are thinking of, e.g. \"What if I increase my monthly savings by 5,000?\" or \"What if I buy a phone for 25,000?\"";
    }

    private String invest(User user, String message) {
        FinancialProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        String riskTolerance = profile != null && profile.getRiskTolerance() != null && !profile.getRiskTolerance().isBlank()
                ? profile.getRiskTolerance() : "Moderate";
        BigDecimal investAmount = null;
        if (message.matches(".*\\d{2,}.*")) investAmount = firstNumber(message);
        if (investAmount == null) {
            investAmount = profile != null && profile.getSavings() != null && profile.getSavings().signum() > 0
                    ? profile.getSavings() : BigDecimal.valueOf(10000);
        }
        int age = ageLowerBound(profile);
        int horizon = profile == null || age < 30 ? 10 : 5;
        InvestmentRecommendationResponse recommendation;
        try {
            recommendation = investmentRecommendationService.recommend(
                    new InvestmentRecommendationRequest(investAmount, horizon, riskTolerance));
        } catch (Exception exception) {
            return "Based on your profile (risk tolerance " + riskTolerance + "), speak to the Investments page for a personalised allocation.";
        }
        StringBuilder response = new StringBuilder("For your profile (" + riskTolerance + " risk, " + horizon + "-year horizon), investing " + money(investAmount) + ":\n");
        recommendation.allocations().forEach(allocation ->
                response.append("• ").append(allocation.assetClass()).append(": ").append(allocation.weightPct())
                        .append("% (").append(money(allocation.amount())).append(")\n"));
        response.append(recommendation.summary()).append("\n");
        response.append("Open the Investments page for the full breakdown and Compare page for products.");
        return response.toString();
    }

    private String learn(User user) {
        var literacy = educationService.literacy(user);
        StringBuilder response = new StringBuilder("Your financial literacy score is " + literacy.literacyScore() + "/100 (" + literacy.level() + ").\n");
        response.append(literacy.summary().isEmpty() ? "" : String.join("\n", literacy.summary()) + "\n");
        response.append("You have completed " + literacy.totalAttempts() + " quiz attempt(s). Topics: UPI safety, phishing detection, card fraud, safe investing and banking hygiene - each ends with a quiz. Try one now!");
        return response.toString();
    }

    private String market(String message) {
        String query = message.replaceAll("(?i)(market|share price|stock price|price of|price|nifty|sensex|gold|trend|today)", "").trim();
        MarketSearchResult first = findSymbolInMessage(message);
        if (first == null) {
            List<MarketSearchResult> results = marketService.search(query.isBlank() ? null : query);
            if (results.isEmpty()) return "I couldn't find that market symbol. Try RELIANCE, TCS, INFY, HDFCBANK, NIFTY50 or GOLDBEES.";
            first = results.get(0);
        }
        try {
            MarketDetailResponse detail = marketService.detail(first.symbol());
            return "Market snapshot for " + detail.name() + " (" + detail.symbol() + "):\n" +
                    "• Trend: " + detail.trend() + " | Risk: " + detail.riskLevel() + "\n" +
                    "• Move over period: " + detail.changePct() + "%\n" +
                    "• Rationale: " + (detail.rationale().isEmpty() ? "" : detail.rationale().get(0)) + "\n" +
                    detail.disclaimer();
        } catch (Exception exception) {
            return "Found " + first.name() + " (" + first.symbol() + "). Open the Markets page for the full snapshot.";
        }
    }

    private MarketSearchResult findSymbolInMessage(String message) {
        String upper = message.toUpperCase(Locale.ROOT);
        List<MarketSearchResult> all = marketService.search(null);
        return all.stream()
                .filter(result -> upper.contains(result.symbol()) || firstNameWord(upper).equals(result.symbol()))
                .findFirst()
                .orElse(null);
    }

    private String firstNameWord(String text) {
        String word = text.replaceAll("[^A-Z]", " ").trim();
        return word.split("\\s+")[0];
    }

    private String scam(User user, String message) {
        if (looksLikeScamSnippet(message)) {
            FraudAnalysis analysis = scamAnalysisService.analyze(user, message, null);
            return "I ran the Scam Scanner on that message:\n• Verdict: " + analysis.getRiskLabel() +
                    "\n• Confidence: " + analysis.getRiskScore() + "/100\n• Why: " + analysis.getSummary() +
                    "\nRecommended actions:\n- " + String.join("; ", com.financialfraudassistant.dto.FraudAnalysisResponse.recommendedActions(analysis.getRiskScore()));
        }
        return "Here is how to spot a scam message quickly:\n• Urgency and threats ('Account will be blocked NOW').\n• Asking for OTP, PIN, KYC or passwords.\n• Shortened or odd links and unknown numbers.\n• Pressure to act immediately or send money.\nPaste any suspicious SMS here and I will scan it for you.";
    }

    private String fallback(User user, String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\b(reliance|tcs|infosys|hdfc|icici|sbi|itc|tatamotors|asianpaint|nifty50|goldbees|infy)\\b.*")) {
            return market(message);
        }
        return "I can check with your real data: scams, health score, spending breakdown, budgets vs actuals, goals, what-if simulations, investments and market snapshots. Could you rephrase that?";
    }

    private boolean looksLikeScamSnippet(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("http") || lower.matches(".*(otp|kyc|pin|password|urgent|blocked|lost|won|lottery|gift|refund|free|last chance|aadhaar|verify).*") && message.length() > 25;
    }

    private BigDecimal firstNumber(String message) {
        String digits = message.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try { return new BigDecimal(digits); }
        catch (NumberFormatException ignored) { return null; }
    }

    private int ageLowerBound(FinancialProfile profile) {
        if (profile == null || profile.getAgeRange() == null) return 35;
        String digits = profile.getAgeRange().replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 35;
        try { return Integer.parseInt(digits.substring(0, Math.min(2, digits.length()))); }
        catch (NumberFormatException ignored) { return 35; }
    }

    private String pct(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) return "";
        return Math.round(part.doubleValue() / whole.doubleValue() * 100) + "%";
    }

    private static String money(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value.doubleValue());
    }
}