package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.IntelligenceResponse;
import com.financialfraudassistant.model.FraudAnalysis;
import com.financialfraudassistant.model.ScamReport;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FraudAnalysisRepository;
import com.financialfraudassistant.repository.ScamReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudIntelligenceServiceTest {

    @Mock private FraudAnalysisRepository analysisRepository;
    @Mock private ScamReportRepository scamReportRepository;

    private FraudIntelligenceService service;
    private final User user = new User("ai@example.com", "hash", "AI Tester");

    private FraudAnalysis analysis(String input, String label) {
        return new FraudAnalysis(user, FraudAnalysis.InputType.TEXT, input, 80, label,
                "Scam", "High", null, "High", "summary");
    }

    private ScamReport report(String description) {
        return new ScamReport(user, "SMS", description, BigDecimal.ZERO, 80);
    }

    @BeforeEach
    void setUp() {
        service = new FraudIntelligenceService(analysisRepository, scamReportRepository);
    }

    @Test
    void emptyCorpus_returnsNoData() {
        when(analysisRepository.findByRiskLabelIn(anyCollection())).thenReturn(List.of());
        when(scamReportRepository.findAll()).thenReturn(List.of());
        when(analysisRepository.countByRiskLabelIn(anyCollection())).thenReturn(0L);
        when(scamReportRepository.count()).thenReturn(0L);

        assertEquals(FraudIntelligenceService.NO_DATA, service.estimate("share your account details immediately"));
    }

    @Test
    void modelWeights_estimateScamHigherThanBenign() {
        when(scamReportRepository.findAll()).thenReturn(List.of(
                report("Account will be blocked please verify update aadhaar share immediately"),
                report("Your account is suspended verify share details immediately")));
        when(analysisRepository.findByRiskLabelIn(List.of("High", "Critical"))).thenReturn(List.of(
                analysis("Account blocked verify share aadhaar details", "High"),
                analysis("Update account verify immediately", "Critical")));
        when(analysisRepository.findByRiskLabelIn(List.of("Low", "Moderate"))).thenReturn(List.of(
                analysis("Monthly statement ready", "Low"),
                analysis("Order shipped shopping complete", "Moderate")));
        when(analysisRepository.countByRiskLabelIn(List.of("High", "Critical"))).thenReturn(2L);
        when(analysisRepository.countByRiskLabelIn(List.of("Low", "Moderate"))).thenReturn(2L);
        when(scamReportRepository.count()).thenReturn(2L);

        int scamEstimate = service.estimate("your account will be blocked please verify and share aadhaar now");
        int benignEstimate = service.estimate("your monthly statement order shipped ready for review");

        assertTrue(scamEstimate > benignEstimate,
                "Expected scam estimate (" + scamEstimate + ") > benign estimate (" + benignEstimate + ")");
        assertTrue(scamEstimate >= 1 && scamEstimate <= 99);
        assertTrue(benignEstimate >= 1 && benignEstimate <= 99);
    }

    @Test
    void topSignals_includeScamLearnedTokens() {
        when(scamReportRepository.findAll()).thenReturn(List.of(
                report("Account blocked share aadhaar details immediately"),
                report("Account suspended verify share aadhaar")));
        when(analysisRepository.findByRiskLabelIn(List.of("High", "Critical"))).thenReturn(List.of(
                analysis("Account blocked verify aadhaar", "Critical")));
        when(analysisRepository.findByRiskLabelIn(List.of("Low", "Moderate"))).thenReturn(List.of(
                analysis("Monthly statement order shipped", "Low")));
        when(analysisRepository.countByRiskLabelIn(List.of("High", "Critical"))).thenReturn(1L);
        when(analysisRepository.countByRiskLabelIn(List.of("Low", "Moderate"))).thenReturn(1L);
        when(scamReportRepository.count()).thenReturn(2L);

        String input = "your account will be blocked please share your aadhaar details now";
        List<String> signals = service.topSignals(input);
        assertFalse(signals.isEmpty(), "Trained model should surface positive signals for scam phrasing");
        assertTrue(signals.stream().anyMatch(signal -> signal.contains("aadhaar") || signal.contains("blocked")),
                "Expected scam-learned token among signals, got " + signals);
    }

    @Test
    void metadata_isConsistent() {
        when(scamReportRepository.findAll()).thenReturn(List.of(
                report("Account blocked share aadhaar details")));
        when(analysisRepository.findByRiskLabelIn(List.of("High", "Critical"))).thenReturn(List.of(
                analysis("Account blocked", "Critical")));
        when(analysisRepository.findByRiskLabelIn(List.of("Low", "Moderate"))).thenReturn(List.of(
                analysis("Monthly statement", "Low")));
        when(analysisRepository.countByRiskLabelIn(List.of("High", "Critical"))).thenReturn(1L);
        when(analysisRepository.countByRiskLabelIn(List.of("Low", "Moderate"))).thenReturn(1L);
        when(scamReportRepository.count()).thenReturn(1L);

        IntelligenceResponse meta = service.metadata();
        assertEquals(0.7, meta.ruleWeight(), 0.001);
        assertEquals(0.3, meta.aiWeight(), 0.001);
        assertTrue(meta.vocabularySize() > 0);
        assertTrue(meta.stages().contains("ML/AI Analysis"));
    }
}
