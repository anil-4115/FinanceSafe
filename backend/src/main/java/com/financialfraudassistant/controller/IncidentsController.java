package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.IncidentResponse;
import com.financialfraudassistant.repository.ScamReportRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
public class IncidentsController {

    private final ScamReportRepository scamReportRepository;

    public IncidentsController(ScamReportRepository scamReportRepository) {
        this.scamReportRepository = scamReportRepository;
    }

    @GetMapping("/incidents")
    public List<IncidentResponse> incidents(Authentication authentication) {
        return scamReportRepository.findAll().stream()
                .sorted(Comparator.comparing(report -> report.getCreatedAt() == null
                        ? java.time.LocalDateTime.MIN : report.getCreatedAt(), Comparator.reverseOrder()))
                .limit(50)
                .map(IncidentResponse::from)
                .toList();
    }
}