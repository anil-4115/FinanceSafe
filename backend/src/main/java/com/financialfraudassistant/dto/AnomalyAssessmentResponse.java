package com.financialfraudassistant.dto;

import java.util.List;

public record AnomalyAssessmentResponse(int riskScore, String riskLevel, List<String> reasons) { }