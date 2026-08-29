package com.financialfraudassistant.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuizAttemptRequest(@NotNull List<Integer> answers) { }