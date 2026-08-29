package com.financialfraudassistant.controller;
import com.financialfraudassistant.dto.AlertResponse;
import com.financialfraudassistant.dto.ScamReportRequest;
import com.financialfraudassistant.model.Alert;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.AlertRepository;
import com.financialfraudassistant.service.CurrentUserService;
import com.financialfraudassistant.service.FraudDetectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FraudController {
    private final CurrentUserService currentUserService; private final AlertRepository alertRepository; private final FraudDetectionService fraudDetectionService;
    public FraudController(CurrentUserService currentUserService, AlertRepository alertRepository, FraudDetectionService fraudDetectionService) { this.currentUserService = currentUserService; this.alertRepository = alertRepository; this.fraudDetectionService = fraudDetectionService; }
    @GetMapping("/alerts") public List<AlertResponse> alerts(Authentication authentication) { User user = currentUserService.requireUser(authentication); return alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(AlertResponse::from).toList(); }
    @PatchMapping("/alerts/{alertId}/resolve") public AlertResponse resolve(Authentication authentication, @PathVariable Integer alertId) { User user = currentUserService.requireUser(authentication); Alert alert = alertRepository.findByIdAndUserId(alertId, user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found")); alert.resolve(); return AlertResponse.from(alertRepository.save(alert)); }
    @PostMapping("/fraud/reports") @ResponseStatus(HttpStatus.CREATED) public Map<String, Integer> report(Authentication authentication, @Valid @RequestBody ScamReportRequest request) { return Map.of("riskScore", fraudDetectionService.analyseScamReport(currentUserService.requireUser(authentication), request)); }
}
