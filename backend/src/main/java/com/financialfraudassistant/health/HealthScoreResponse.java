package com.financialfraudassistant.health;

import java.util.List;

public record HealthScoreResponse(
        int score,
        String label,
        List<ComponentScore> components,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations) {

    public record ComponentScore(String name, int score, int weight, String note) { }
}
