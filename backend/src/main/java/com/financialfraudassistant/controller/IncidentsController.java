package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.IncidentResponse;
import com.financialfraudassistant.repository.ScamReportRepository;
import com.financialfraudassistant.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IncidentsController {

    private final ScamReportRepository scamReportRepository;
    private final CurrentUserService currentUserService;

    public IncidentsController(ScamReportRepository scamReportRepository, CurrentUserService currentUserService) {
        this.scamReportRepository = scamReportRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/incidents")
    public List<IncidentResponse> incidents(Authentication authentication) {
        return scamReportRepository.findByUserIdOrderByCreatedAtDesc(currentUserService.requireUser(authentication).getId())
                .stream()
                .limit(50)
                .map(IncidentResponse::from)
                .toList();
    }
}