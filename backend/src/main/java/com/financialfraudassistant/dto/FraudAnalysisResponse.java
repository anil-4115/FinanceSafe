package com.financialfraudassistant.dto;

import com.financialfraudassistant.model.FraudAnalysis;
import com.financialfraudassistant.model.FraudIndicator;

import java.time.LocalDateTime;
import java.util.List;

public record FraudAnalysisResponse(
        Integer id,
        String inputType,
        String input,
        int riskScore,
        String riskLevel,
        String scamType,
        String confidence,
        String summary,
        List<FraudIndicatorResponse> indicators,
        List<String> recommendedActions,
        LocalDateTime createdAt) {

    public static FraudAnalysisResponse from(FraudAnalysis analysis, List<FraudIndicator> indicators) {
        return new FraudAnalysisResponse(
                analysis.getId(),
                analysis.getInputType().name(),
                analysis.getInput(),
                analysis.getRiskScore(),
                analysis.getRiskLabel(),
                analysis.getScamType(),
                analysis.getConfidence(),
                analysis.getSummary(),
                indicators.stream().map(indicator -> new FraudIndicatorResponse(indicator.getKind(), indicator.getLabel(), indicator.getWeight())).toList(),
                recommendedActions(analysis.getRiskScore()),
                analysis.getCreatedAt());
    }

    public static List<String> recommendedActions(int riskScore) {
        if (riskScore < 25) {
            return List.of("This looks harmless, but treat every unexpected message with caution.");
        }
        if (riskScore < 60) {
            return List.of("Do not respond with personal details such as OTPs, PINs or passwords.",
                    "Verify the sender through an official app or website before acting.",
                    "If in doubt, ignore and delete the message.");
        }
        return List.of("Do not click any link, download any file, or share your OTP/UPI PIN.",
                "Open the official banking application or website manually — never via links in the message.",
                "Verify through official support channels before taking any step.",
                "Report/block the sender and inform your bank if money has already moved.",
                "Keep a screenshot as evidence for a formal incident report.");
    }
}