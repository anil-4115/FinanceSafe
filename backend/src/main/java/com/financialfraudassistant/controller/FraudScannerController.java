package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.AnalyzeFraudRequest;
import com.financialfraudassistant.dto.AnomalyAssessmentResponse;
import com.financialfraudassistant.dto.FraudAnalysisResponse;
import com.financialfraudassistant.dto.TransactionRiskRequest;
import com.financialfraudassistant.model.FraudAnalysis;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FraudAnalysisRepository;
import com.financialfraudassistant.service.AnomalyService;
import com.financialfraudassistant.service.CurrentUserService;
import com.financialfraudassistant.service.ScamAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/fraud")
public class FraudScannerController {

    private final CurrentUserService currentUserService;
    private final ScamAnalysisService scamAnalysisService;
    private final AnomalyService anomalyService;
    private final FraudAnalysisRepository analysisRepository;

    public FraudScannerController(CurrentUserService currentUserService, ScamAnalysisService scamAnalysisService,
                                  AnomalyService anomalyService, FraudAnalysisRepository analysisRepository) {
        this.currentUserService = currentUserService;
        this.scamAnalysisService = scamAnalysisService;
        this.anomalyService = anomalyService;
        this.analysisRepository = analysisRepository;
    }

    @PostMapping("/analyze")
    public FraudAnalysisResponse analyze(Authentication authentication, @Valid @RequestBody AnalyzeFraudRequest request) {
        User user = currentUserService.requireUser(authentication);
        FraudAnalysis analysis = scamAnalysisService.analyze(user, request.content(), request.type());
        return FraudAnalysisResponse.from(analysis, analysis.getIndicators());
    }

    @PostMapping("/transaction-risk")
    public AnomalyAssessmentResponse transactionRisk(Authentication authentication, @Valid @RequestBody TransactionRiskRequest request) {
        User user = currentUserService.requireUser(authentication);
        AnomalyService.Result result = anomalyService.assess(user, null, request.merchant(), request.amount(),
                request.category(), request.transactionDate());
        return new AnomalyAssessmentResponse(result.score(), result.level(), result.reasons());
    }

    @GetMapping("/history")
    public List<FraudAnalysisResponse> history(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        return analysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(analysis -> FraudAnalysisResponse.from(analysis, analysis.getIndicators()))
                .toList();
    }

    @GetMapping("/history/{id}")
    public FraudAnalysisResponse detail(Authentication authentication, @PathVariable Integer id) {
        User user = currentUserService.requireUser(authentication);
        FraudAnalysis analysis = analysisRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"));
        return FraudAnalysisResponse.from(analysis, analysis.getIndicators());
    }
}