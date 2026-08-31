package com.financialfraudassistant.health;

import com.financialfraudassistant.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health-score")
public class FinancialHealthController {

    private final CurrentUserService currentUserService;
    private final HealthScoreService healthScoreService;

    public FinancialHealthController(CurrentUserService currentUserService, HealthScoreService healthScoreService) {
        this.currentUserService = currentUserService;
        this.healthScoreService = healthScoreService;
    }

    @GetMapping
    public HealthScoreResponse evaluate(Authentication authentication) {
        return healthScoreService.evaluate(currentUserService.requireUser(authentication));
    }
}
