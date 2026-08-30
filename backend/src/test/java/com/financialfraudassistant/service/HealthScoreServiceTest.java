package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.HealthScoreResponse;
import com.financialfraudassistant.model.FinancialGoal;
import com.financialfraudassistant.model.FinancialProfile;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.BudgetRepository;
import com.financialfraudassistant.repository.FinancialGoalRepository;
import com.financialfraudassistant.repository.FinancialProfileRepository;
import com.financialfraudassistant.repository.FinancialTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthScoreServiceTest {

    @Mock private FinancialProfileRepository profileRepository;
    @Mock private FinancialTransactionRepository transactionRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private FinancialGoalRepository goalRepository;
    @Mock private FinanceAnalyticsService analytics;

    private HealthScoreService service;
    private final User user = new User("health@example.com", "hash", "Health Tester");

    @BeforeEach
    void setUp() {
        service = new HealthScoreService(profileRepository, transactionRepository, budgetRepository, goalRepository, analytics);
    }

    private FinancialProfile profile(BigDecimal income, BigDecimal expense, BigDecimal savings,
                                     BigDecimal investments, BigDecimal debt, String categories) {
        FinancialProfile profile = new FinancialProfile(user);
        profile.update("25-34", "Salaried", income, expense, savings, investments, debt, "Moderate", "Intermediate", categories);
        return profile;
    }

    private void emptyData() {
        when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(any())).thenReturn(List.of());
        when(budgetRepository.findByUserIdOrderByCategory(any())).thenReturn(List.of());
        when(goalRepository.findByUserIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(analytics.averageMonthlyIncome(any())).thenReturn(BigDecimal.ZERO);
        when(analytics.averageMonthlyExpense(any())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void noProfile_noData_returnsManagedScoreInRange() {
        emptyData();
        HealthScoreResponse result = service.evaluate(user);

        assertTrue(result.score() >= 0 && result.score() <= 100, "Score must be within 0..100, got " + result.score());
        assertNotNull(result.label());
        assertTrue(result.components().stream().allMatch(c -> c.score() >= 0 && c.score() <= 100));
        assertTrue(result.recommendations().stream().anyMatch(r -> r.toLowerCase().contains("profile")),
                "No-profile case should recommend completing the financial profile");
    }

    @Test
    void healthyProfile_scoresHigherThan_poorProfile() {
        FinancialProfile healthy = profile(new BigDecimal("100000"), new BigDecimal("30000"),
                new BigDecimal("450000"), new BigDecimal("200000"), BigDecimal.ZERO, "Mutual funds,Stocks,FD");
        when(profileRepository.findByUserId(any())).thenReturn(Optional.of(healthy));
        when(transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(any())).thenReturn(List.of());
        when(budgetRepository.findByUserIdOrderByCategory(any())).thenReturn(List.of());
        when(goalRepository.findByUserIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        HealthScoreResponse healthyResult = service.evaluate(user);

        FinancialProfile poor = profile(new BigDecimal("40000"), new BigDecimal("35000"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("500000"), null);
        when(profileRepository.findByUserId(any())).thenReturn(Optional.of(poor));
        HealthScoreResponse poorResult = service.evaluate(user);

        assertTrue(healthyResult.score() > poorResult.score(),
                "Healthy profile (" + healthyResult.score() + ") should outscore poor profile (" + poorResult.score() + ")");
        assertTrue(healthyResult.score() >= 70, "Healthy profile should be GOOD+, got " + healthyResult.score());
        assertTrue(poorResult.score() < 50, "Poor profile should be below average, got " + poorResult.score());
        assertTrue(healthyResult.strengths().size() > poorResult.strengths().size());
    }

    @Test
    void zeroSavings_generatesMeaningfulRecommendation() {
        FinancialProfile profile = profile(new BigDecimal("50000"), new BigDecimal("30000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        when(profileRepository.findByUserId(any())).thenReturn(Optional.of(profile));
        when(transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(any())).thenReturn(List.of());
        when(budgetRepository.findByUserIdOrderByCategory(any())).thenReturn(List.of());
        when(goalRepository.findByUserIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        HealthScoreResponse result = service.evaluate(user);

        assertTrue(result.recommendations().stream().anyMatch(r -> r.toLowerCase().contains("savings")),
                "Zero-savings profile should recommend recording savings");
    }

    @Test
    void overallGoalProgress_returnsCorrectPercentage() {
        when(goalRepository.findByUserIdOrderByCreatedAtDesc(any())).thenReturn(List.of(
                new FinancialGoal(user, "Vacation", new BigDecimal("20000"), new BigDecimal("5000"),
                        LocalDate.of(2027, 6, 1), new BigDecimal("2000")),
                new FinancialGoal(user, "Laptop", new BigDecimal("50000"), new BigDecimal("15000"),
                        LocalDate.of(2026, 12, 1), new BigDecimal("3000"))));

        BigDecimal progress = service.overallGoalProgress(user);
        assertEquals(28.6, progress.doubleValue(), 0.1);
    }

    @Test
    void noGoals_returnsZeroProgress() {
        when(goalRepository.findByUserIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        assertEquals(0, service.overallGoalProgress(user).doubleValue(), 0.001);
    }
}
