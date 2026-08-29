package com.financialfraudassistant.dto;

public record EducationModuleResponse(
        int id,
        String title,
        String topic,
        String category,
        String content,
        int durationMins,
        Integer bestScorePct) { }