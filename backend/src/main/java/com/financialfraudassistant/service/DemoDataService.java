package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.DemoDataResponse;
import com.financialfraudassistant.model.Alert;
import com.financialfraudassistant.model.Budget;
import com.financialfraudassistant.model.ChatMessage;
import com.financialfraudassistant.model.EducationAttempt;
import com.financialfraudassistant.model.FinancialGoal;
import com.financialfraudassistant.model.FinancialProfile;
import com.financialfraudassistant.model.FinancialTransaction;
import com.financialfraudassistant.model.ScamReport;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.AlertRepository;
import com.financialfraudassistant.repository.BudgetRepository;
import com.financialfraudassistant.repository.ChatConversationRepository;
import com.financialfraudassistant.repository.ChatMessageRepository;
import com.financialfraudassistant.repository.EducationAttemptRepository;
import com.financialfraudassistant.repository.FinancialGoalRepository;
import com.financialfraudassistant.repository.FinancialProfileRepository;
import com.financialfraudassistant.repository.FinancialTransactionRepository;
import com.financialfraudassistant.repository.FraudAnalysisRepository;
import com.financialfraudassistant.repository.FraudIndicatorRepository;
import com.financialfraudassistant.repository.ScamReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class DemoDataService {

    private final FinancialProfileRepository profileRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final FinancialGoalRepository goalRepository;
    private final AlertRepository alertRepository;
    private final ScamReportRepository scamReportRepository;
    private final FraudAnalysisRepository fraudAnalysisRepository;
    private final FraudIndicatorRepository fraudIndicatorRepository;
    private final EducationAttemptRepository attemptRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    public DemoDataService(FinancialProfileRepository profileRepository, FinancialTransactionRepository transactionRepository,
                           BudgetRepository budgetRepository, FinancialGoalRepository goalRepository, AlertRepository alertRepository,
                           ScamReportRepository scamReportRepository, FraudAnalysisRepository fraudAnalysisRepository,
                           FraudIndicatorRepository fraudIndicatorRepository, EducationAttemptRepository attemptRepository,
                           ChatConversationRepository conversationRepository, ChatMessageRepository messageRepository) {
        this.profileRepository = profileRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.alertRepository = alertRepository;
        this.scamReportRepository = scamReportRepository;
        this.fraudAnalysisRepository = fraudAnalysisRepository;
        this.fraudIndicatorRepository = fraudIndicatorRepository;
        this.attemptRepository = attemptRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public DemoDataResponse loadSample(User user) {
        if (transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(user.getId()).size() > 0) {
            return new DemoDataResponse("You already have transaction data. Use 'Replace with sample data' on the demo page instead.", 0, 0, 0, 0, true);
        }

        profileRepository.findByUserId(user.getId()).orElseGet(() -> {
            FinancialProfile profile = new FinancialProfile(user);
            profile.update("26-35", "Salaried", new BigDecimal("65000"), new BigDecimal("28000"),
                    new BigDecimal("46000"), new BigDecimal("120000"), new BigDecimal("0"),
                    "MODERATE", "Intermediate", "Equity,Debt,Gold,Fixed deposits");
            return profileRepository.save(profile);
        });

        budgetRepository.deleteAll(budgetRepository.findByUserIdOrderByCategory(user.getId()));
        budgetRepository.save(new Budget(user, "Grocery", new BigDecimal("8000")));
        budgetRepository.save(new Budget(user, "Dining out", new BigDecimal("4000")));
        budgetRepository.save(new Budget(user, "Entertainment", new BigDecimal("3000")));
        budgetRepository.save(new Budget(user, "Transport", new BigDecimal("3500")));

        goalRepository.deleteAll(goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        goalRepository.save(new FinancialGoal(user, "Emergency fund", new BigDecimal("150000"), new BigDecimal("46000"),
                LocalDate.now().plusMonths(18), new BigDecimal("5000")));
        goalRepository.save(new FinancialGoal(user, "Trip to Goa", new BigDecimal("60000"), new BigDecimal("18500"),
                LocalDate.now().plusMonths(9), new BigDecimal("3000")));

        createTransactions(user);

        alertRepository.deleteAll(alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        alertRepository.save(new Alert(user, "Budgets protected",
                "Sample: you have 4 active budgets this month. Keep spending within limits to stay protected.",
                Alert.Severity.INFO, "SAMPLE", 10));

        int txCount = transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(user.getId()).size();
        return new DemoDataResponse(
                "Sample data loaded: realistic transactions, budgets, goals and a profile are now ready to explore. Every number comes from the sample transactions below.",
                txCount, 4, 2, 1, false);
    }

    private void createTransactions(User user) {
        LocalDate today = LocalDate.now();
        // Monthly salary for the last 3 months
        for (int month = 2; month >= 0; month--) {
            LocalDate monthStart = today.minusMonths(month).with(TemporalAdjusters.firstDayOfMonth());
            transactionRepository.save(new FinancialTransaction(user, monthStart,
                    "Employer Salary", new BigDecimal("65000"), FinancialTransaction.Type.INCOME,
                    "Salary", FinancialTransaction.Source.MANUAL, "Monthly salary"));
        }
        addExpense(user, today.minusDays(0), "BigBasket", "1200", "Grocery");
        addExpense(user, today.minusDays(1), "Swiggy", "480", "Dining out");
        addExpense(user, today.minusDays(2), "PVR Cinemas", "750", "Entertainment");
        addExpense(user, today.minusDays(3), "Indian Oil", "1500", "Transport");
        addExpense(user, today.minusDays(6), "Amazon", "6400", "Shopping");
        addExpense(user, today.minusDays(9), "Apollo Pharmacy", "820", "Healthcare");
        addExpense(user, today.minusDays(12), "Jio Recharge", "299", "Bills");
        addExpense(user, today.minusDays(15), "Zomato", "560", "Dining out");
        addExpense(user, today.minusDays(19), "BigBasket", "1750", "Grocery");
        addExpense(user, today.minusDays(23), "Netflix", "649", "Entertainment");
        addExpense(user, today.minusDays(27), "Metro Card", "300", "Transport");
        addExpense(user, today.minusDays(33), "Rent", "12000", "Housing");
        addExpense(user, today.minusDays(38), "1MG", "640", "Healthcare");
        addExpense(user, today.minusDays(44), "DMart", "2450", "Grocery");
        addExpense(user, today.minusDays(51), "Uber", "420", "Transport");
        addExpense(user, today.minusDays(58), "Bata", "2199", "Shopping");
        addExpense(user, today.minusDays(65), "Rent", "12000", "Housing");
        addExpense(user, today.minusDays(72), "BigBasket", "1680", "Grocery");
    }

    private void addExpense(User user, LocalDate date, String merchant, String amount, String category) {
        transactionRepository.save(new FinancialTransaction(user, date, merchant,
                new BigDecimal(amount), FinancialTransaction.Type.EXPENSE, category, FinancialTransaction.Source.MANUAL, ""));
    }

    @Transactional
    public DemoDataResponse clear(User user) {
        List<FinancialTransaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(user.getId());
        for (var analysis : fraudAnalysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            fraudIndicatorRepository.deleteAll(analysis.getIndicators());
        }
        fraudAnalysisRepository.deleteAll(fraudAnalysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));

        scamReportRepository.deleteAll(scamReportRepository.findAll().stream()
                .filter(report -> report.getUser() != null && report.getUser().getId().equals(user.getId())).toList());

        List<ChatMessage> messages = messageRepository.findAll().stream()
                .filter(message -> message.getConversation() != null && message.getConversation().getUser() != null
                        && message.getConversation().getUser().getId().equals(user.getId())).toList();
        if (!messages.isEmpty()) messageRepository.deleteAll(messages);
        conversationRepository.deleteAll(conversationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));

        attemptRepository.deleteAll(attemptRepository.findByUserId(user.getId()));
        alertRepository.deleteAll(alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        budgetRepository.deleteAll(budgetRepository.findByUserIdOrderByCategory(user.getId()));
        goalRepository.deleteAll(goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        profileRepository.findByUserId(user.getId()).ifPresent(profileRepository::delete);
        transactionRepository.deleteAll(transactions);

        return new DemoDataResponse("All your data has been cleared. You can reload the sample dataset any time.", 0, 0, 0, 0, false);
    }
}