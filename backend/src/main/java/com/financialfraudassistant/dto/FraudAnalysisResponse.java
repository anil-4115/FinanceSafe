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
            return List.of("This looks low risk. Still open the official app yourself if you need to check anything.");
        }
        if (riskScore < 50) {
            return List.of("Do not share OTP, PIN, passwords or KYC documents.",
                    "Verify the sender through the official app or website — never via this message.",
                    "If in doubt, ignore and delete the message.");
        }
        if (riskScore < 75) {
            return List.of("Treat this as a likely scam. Do not click links or approve payments.",
                    "Do not share OTP, PIN or personal documents.",
                    "Verify only through official channels you already trust.",
                    "Keep a screenshot if you need to report it.");
        }
        return List.of("Do not click the link or share credentials/OTP.",
                "Do not transfer money or pay any processing fee.",
                "Verify through the official banking app or a number printed on your card.",
                "Report the sender, and if money has moved call 1930 / cybercrime.gov.in.");
    }
}