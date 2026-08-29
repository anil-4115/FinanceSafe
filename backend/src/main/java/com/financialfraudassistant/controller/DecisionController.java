package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.DecisionRequest;
import com.financialfraudassistant.dto.DecisionResponse;
import com.financialfraudassistant.service.CurrentUserService;
import com.financialfraudassistant.service.DecisionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/decision")
public class DecisionController {

    private final CurrentUserService currentUserService;
    private final DecisionService decisionService;

    public DecisionController(CurrentUserService currentUserService, DecisionService decisionService) {
        this.currentUserService = currentUserService;
        this.decisionService = decisionService;
    }

    @PostMapping("/analyze")
    public DecisionResponse analyze(Authentication authentication, @Valid @RequestBody DecisionRequest request) {
        return decisionService.analyze(currentUserService.requireUser(authentication), request);
    }
}