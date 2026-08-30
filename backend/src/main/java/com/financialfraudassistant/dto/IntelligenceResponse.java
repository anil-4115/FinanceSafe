package com.financialfraudassistant.dto;

import java.time.LocalDateTime;
import java.util.List;

public record IntelligenceResponse(
        List<String> stages,
        double ruleWeight,
        double aiWeight,
        double baseRate,
        long scamDocuments,
        long benignDocuments,
        long vocabularySize,
        LocalDateTime modelBuiltAt,
        List<String> topSignals) {
}