package com.financialfraudassistant.dto;

import java.util.List;

public record QuizAttemptResponse(
        int scorePct,
        int correct,
        int total,
        int literacyScore,
        List<QuestionResult> results) {

    public record QuestionResult(int questionId, String question, String yourAnswer, String correctAnswer, boolean correct, String explanation) { }
}