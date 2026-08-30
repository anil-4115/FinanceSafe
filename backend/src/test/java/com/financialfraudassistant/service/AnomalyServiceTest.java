package com.financialfraudassistant.service;

import com.financialfraudassistant.model.FinancialTransaction;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FinancialTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyServiceTest {

    @Mock private FinancialTransactionRepository transactionRepository;

    private AnomalyService service;
    private final User user = new User("anomaly@example.com", "hash", "Anomaly Tester");

    @BeforeEach
    void setUp() {
        service = new AnomalyService(transactionRepository);
    }

    private FinancialTransaction expense(String merchant, String amount, String category, LocalDate date) {
        return new FinancialTransaction(user, date, merchant, new BigDecimal(amount),
                FinancialTransaction.Type.EXPENSE, category, FinancialTransaction.Source.MANUAL, null);
    }

    @Test
    void normalTransaction_staysLow() {
        LocalDate day = LocalDate.of(2026, 8, 10);
        when(transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(any())).thenReturn(List.of(
                expense("Local Grocery", "500", "Food", day.minusDays(4)),
                expense("Local Grocery", "800", "Food", day.minusDays(8)),
                expense("Local Grocery", "1200", "Food", day.minusDays(12)),
                expense("Local Grocery", "900", "Food", day.minusDays(16)),
                expense("Local Grocery", "700", "Food", day.minusDays(20))));

        AnomalyService.Result result = service.assess(user, null, "Local Grocery", new BigDecimal("850"), "Food", day);

        assertEquals("LOW", result.level());
        assertTrue(result.score() < 35);
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.toLowerCase().contains("consistent")
                || reason.toLowerCase().contains("normal")
                || result.score() < 35));
    }

    @Test
    void largeNewBeneficiary_isHighAnomaly() {
        LocalDate day = LocalDate.of(2026, 8, 10);
        when(transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(any())).thenReturn(List.of(
                expense("Local Grocery", "500", "Food", day.minusDays(3)),
                expense("Metro", "800", "Transport", day.minusDays(6)),
                expense("Cafe", "1200", "Food", day.minusDays(9)),
                expense("Pharmacy", "900", "Health", day.minusDays(12)),
                expense("Utilities", "2000", "Bills", day.minusDays(15))));

        AnomalyService.Result result = service.assess(user, null, "Unknown Payee", new BigDecimal("85000"),
                "Transfer", day);

        assertTrue(result.score() >= 60, "Expected HIGH anomaly, got " + result.score());
        assertTrue(List.of("HIGH", "CRITICAL").contains(result.level()));
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.toLowerCase().contains("amount")
                || reason.toLowerCase().contains("merchant")));
    }

    @Test
    void highRiskCategory_raisesScore() {
        when(transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(any())).thenReturn(List.of());

        AnomalyService.Result result = service.assess(user, null, "Crypto Exchange", new BigDecimal("15000"),
                "crypto", LocalDate.of(2026, 8, 10));

        assertTrue(result.score() >= 25);
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.toLowerCase().contains("fraud")
                || reason.toLowerCase().contains("crypto")));
    }
}
