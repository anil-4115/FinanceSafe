package com.financialfraudassistant.dto;

import java.util.List;

public record LiteracyResponse(int literacyScore, String level, int totalAttempts, List<String> summary) { }