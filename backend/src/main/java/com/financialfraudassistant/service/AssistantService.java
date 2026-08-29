package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.AssistantRequest;
import com.financialfraudassistant.dto.AssistantResponse;
import com.financialfraudassistant.model.ChatConversation;
import com.financialfraudassistant.model.ChatMessage;
import com.financialfraudassistant.model.FraudAnalysis;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.BudgetRepository;
import com.financialfraudassistant.repository.ChatConversationRepository;
import com.financialfraudassistant.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AssistantService {

    private enum Intent { GREETING, SCAM_ANALYZE, HEALTH, GOAL, SPENDING, RISK, BUDGET, WHAT_IF, INVEST, LEARN, FALLBACK }

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final FinanceAnalyticsService analytics;
    private final HealthScoreService healthScoreService;
    private final ScamAnalysisService scamAnalysisService;
    private final WhatIfService whatIfService;
    private final BudgetRepository budgetRepository;

    public AssistantService(ChatConversationRepository conversationRepository, ChatMessageRepository messageRepository,
                            FinanceAnalyticsService analytics, HealthScoreService healthScoreService,
                            ScamAnalysisService scamAnalysisService, WhatIfService whatIfService,
                            BudgetRepository budgetRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.analytics = analytics;
        this.healthScoreService = healthScoreService;
        this.scamAnalysisService = scamAnalysisService;
        this.whatIfService = whatIfService;
        this.budgetRepository = budgetRepository;
    }

    private static final List<String> SUGGESTED = List.of(
            "Is this SMS a scam? Your Aadhaar is blocked, share OTP",
            "How is my financial health?",
            "Where am I overspending?",
            "What if I save Rs 5,000 more each month?");

    public AssistantResponse handle(User user, AssistantRequest request) {
        String message = request.message() == null ? "" : request.message().trim();
        if (message.isBlank()) {
            return reply(user, message, "greeting",
                    "Hi! I'm FinanceSafe. Ask me things like: 'Check if this message is a scam', 'How is my financial health?', 'Where am I overspending?', or 'What if I save 5,000 more a month?'");
        }
        Intent intent = classify(message);
        String body = switch (intent) {
            case GREETING -> greeting();
            case HEALTH -> health(user);
            case GOAL -> goal(user);
            case SPENDING -> spending(user);
            case RISK -> risk(user);
            case BUDGET -> budget(user);
            case WHAT_IF -> whatIf(user, message);
            case INVEST -> invest(user);
            case LEARN -> learn(user);
            case SCAM_ANALYZE -> scam(user, message);
            case FALLBACK -> fallback();
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

    private Intent classify(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.matches(".*(hi|hello|hey|namaste|help me).*")) return Intent.GREETING;
        if (lower.contains("scam") || lower.contains("fraud") || lower.contains("phishing") || lower.contains("otp")
                || lower.contains("kyc") || lower.contains("is this") || lower.contains("message") || lower.contains("sms")
                || lower.contains("urgent") || lower.contains("lottery") || lower.contains("bank")) return Intent.SCAM_ANALYZE;
        if (lower.contains("health") || lower.contains("score") || lower.contains("how am i doing")) return Intent.HEALTH;
        if (lower.contains("goal") || lower.contains("achieve") || lower.contains("save for") || lower.contains("on track")
                || lower.contains("reach")) return Intent.GOAL;
        if (lower.contains("where") || lower.contains("spend") || lower.contains("overspend") || lower.contains("categories")
                || lower.contains("too much")) return Intent.SPENDING;
        if (lower.contains("risk") || lower.contains("flagged") || lower.contains("suspicious") || lower.contains("why did")) return Intent.RISK;
        if (lower.contains("budget") || lower.contains("over budget") || lower.contains("limit")) return Intent.BUDGET;
        if (lower.contains("what if") || lower.contains("save more") || lower.contains("increase my savings")
                || lower.contains("reduce spending") || lower.contains("afford")) return Intent.WHAT_IF;
        if (lower.contains("invest") || lower.contains("sip") || lower.contains("fd") || lower.contains("fund")
                || lower.contains("return") || lower.contains("scheme") || lower.contains("stock")) return Intent.INVEST;
        if (lower.contains("learn") || lower.contains("lesson") || lower.contains("module") || lower.contains("quiz")
                || lower.contains("education") || lower.contains("teaching") || lower.contains("compound") || lower.contains("upi")) return Intent.LEARN;
        return Intent.FALLBACK;
    }

    private String greeting() {
        return "Hi! I'm FinanceSafe, your fraud guard and money coach. You can ask me to:" +
                "\n• Check if a message, SMS or link looks like a scam" +
                "\n• Explain your financial health score" +
                "\n• Break down where your money goes" +
                "\n• Simulate a decision before you make it";
    }

    private String health(User user) {
        var result = healthScoreService.evaluate(user);
        StringBuilder response = new StringBuilder("Your financial health score is " + result.score() + "/100.");
        if (result.strengths() != null && !result.strengths().isEmpty()) {
            response.append("\nStrengths:\n- ").append(String.join("\n- ", result.strengths()));
        }
        if (result.weaknesses() != null && !result.weaknesses().isEmpty()) {
            response.append("\nAreas to improve:\n- ").append(String.join("\n- ", result.weaknesses()));
        }
        response.append("\nTop suggestion: ").append(result.recommendations().get(0));
        return response.toString();
    }

    private String goal(User user) {
        BigDecimal progress = healthScoreService.overallGoalProgress(user);
        return "Across your current savings goals you have contributed about " + money(progress) +
                ". Keep consistent monthly contributions - consistency beats big one-time deposits." +
                " Try the 'What-if' simulator to see how a small boost to your monthly savings accelerates your goals.";
    }

    private String spending(User user) {
        StringBuilder response = new StringBuilder("Here is your recent spending by category (average monthly):\n");
        var breakdown = analytics.categoryBreakdown(user);
        if (breakdown.isEmpty()) return "You don't have enough transactions yet. Add a few in the Transactions page and I can analyse where your money goes.";
        breakdown.forEach(entry -> response.append("• ").append(entry.category()).append(": ").append(money(entry.amount())).append("\n"));
        return response.toString();
    }

    private String risk(User user) {
        int risky = (int) analytics.transactions(user).stream().filter(item -> item.getRiskScore() != null && item.getRiskScore() >= 50).count();
        return "Currently " + risky + " of your transactions carry a risk score of 50 or above. Risky or flagged items appear in the Alerts page." +
                " If you want more detail, open the Fraud History page which explains exactly why each item was flagged.";
    }

    private String budget(User user) {
        var budgets = budgetRepository.findByUserIdOrderByCategory(user.getId());
        if (budgets.isEmpty()) return "You haven't set any budgets yet - go to the Budget page and set limits by category so I can warn you when you are spending too much.";
        var total = budgets.stream().map(budget -> budget.getMonthlyLimit() == null ? BigDecimal.ZERO : budget.getMonthlyLimit()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return "You have " + budgets.size() + " active budget(s) covering a total of " + money(total) +
                " per month. I compare your actual category spending against these limits so you are alerted before you overshoot.";
    }

    private String whatIf(User user, String message) {
        if (message.matches(".*\\d{2,}.*")) {
            BigDecimal amount = firstNumber(message);
            String type = message.contains("save") || message.contains("invest") ? "INCREASE_SAVINGS"
                    : message.contains("spend") ? "DECREASE_SPENDING" : "ONE_TIME_PURCHASE";
            var result = whatIfService.simulate(user, new com.financialfraudassistant.dto.WhatIfRequest(type, amount, null));
            return "Simulation for " + money(amount) + ":\n• Health score: " + result.healthBefore() + " → " + result.healthAfter() +
                    "\n• Savings: " + money(result.savingsBefore()) + " → " + money(result.savingsAfter()) +
                    "\n" + result.explanations().get(0);
        }
        return "Tell me an amount and what you are thinking of, e.g. \"What if I increase my monthly savings by 5,000?\" or \"What if I buy a phone for 25,000?\"";
    }

    private String invest(User user) {
        return "Some investing basics:\n• Compounding: money grows on both your deposits and the returns they earned.\n" +
                "• Diversify: spread across equity, debt and gold instead of one option.\n" +
                "• Match the timeline: 5+ years suits equity; 1-3 years fits debt; savings you may need now stay liquid.\n" +
                "Open the Investments page for a personalised allocation, and the Compare page to see products side by side.";
    }

    private String learn(User user) {
        return "The Learning Library has short lessons with quizzes covering UPI safety, spotting phishing, card fraud and safe investing." +
                " Completing quizzes builds your Financial Literacy score - try the Scam Scanner test too, it trains you on real scam patterns.";
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

    private String fallback() {
        return "I can help with: scam checks, your health score, spending breakdown, budgets, goals, what-if simulations, investing basics and learning lessons. Could you rephrase that?";
    }

    private boolean looksLikeScamSnippet(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("http") || lower.matches(".*(otp|kyc|pin|password|urgent|blocked|lost|won|lottery|gift|refund|free|last chance).*") && message.length() > 25;
    }

    private BigDecimal firstNumber(String message) {
        String digits = message.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return BigDecimal.valueOf(5000);
        try { return new BigDecimal(digits); }
        catch (NumberFormatException ignored) { return BigDecimal.valueOf(5000); }
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value.doubleValue());
    }
}