package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.InvestmentRecommendationRequest;
import com.financialfraudassistant.dto.InvestmentRecommendationResponse;
import com.financialfraudassistant.service.CurrentUserService;
import com.financialfraudassistant.service.InvestmentRecommendationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentRecommendationService recommendationService;

    public InvestmentController(InvestmentRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/recommendation")
    public InvestmentRecommendationResponse recommend(@Valid @RequestBody InvestmentRecommendationRequest request) {
        return recommendationService.recommend(request);
    }
}