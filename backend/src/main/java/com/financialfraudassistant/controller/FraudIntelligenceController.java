package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.IntelligenceAnalyzeRequest;
import com.financialfraudassistant.dto.IntelligenceResponse;
import com.financialfraudassistant.service.FraudIntelligenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Exposes the "ML/AI Analysis" stage of the fraud-intelligence pipeline as an
 * authenticated API. This is the hybrid (rules + learned model) signal that
 * complements the deterministic rule engine used by the Scam Scanner.
 */
@RestController
@RequestMapping("/api/fraud/intelligence")
public class FraudIntelligenceController {

    private final FraudIntelligenceService intelligenceService;

    public FraudIntelligenceController(FraudIntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping
    public IntelligenceResponse metadata() {
        return intelligenceService.metadata();
    }

    @PostMapping("/analyze")
    public Map<String, Object> analyze(@Valid @RequestBody IntelligenceAnalyzeRequest request) {
        String content = request.content();
        return Map.of(
                "estimate", intelligenceService.estimate(content),
                "signals", intelligenceService.topSignals(content));
    }
}
