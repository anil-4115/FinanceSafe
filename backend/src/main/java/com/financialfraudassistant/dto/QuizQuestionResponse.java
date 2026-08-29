package com.financialfraudassistant.dto;

import java.util.List;

public record QuizQuestionResponse(int id, String question, List<String> options) { }