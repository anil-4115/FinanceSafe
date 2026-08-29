package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.InvestmentSimulationRequest;
import com.financialfraudassistant.dto.InvestmentSimulationResponse;
import com.financialfraudassistant.dto.WhatIfRequest;
import com.financialfraudassistant.dto.WhatIfResponse;
import com.financialfraudassistant.service.CurrentUserService;
import com.financialfraudassistant.service.InvestmentSimulatorService;
import com.financialfraudassistant.service.WhatIfService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final CurrentUserService currentUserService;
    private final InvestmentSimulatorService investmentSimulator;
    private final WhatIfService whatIfService;

    public SimulatorController(CurrentUserService currentUserService, InvestmentSimulatorService investmentSimulator,
                               WhatIfService whatIfService) {
        this.currentUserService = currentUserService;
        this.investmentSimulator = investmentSimulator;
        this.whatIfService = whatIfService;
    }

    @PostMapping("/investment")
    public InvestmentSimulationResponse investment(@Valid @RequestBody InvestmentSimulationRequest request) {
        return investmentSimulator.simulate(request);
    }

    @PostMapping("/what-if")
    public WhatIfResponse whatIf(Authentication authentication, @Valid @RequestBody WhatIfRequest request) {
        return whatIfService.simulate(currentUserService.requireUser(authentication), request);
    }
}