package com.financialfraudassistant.dto;

import java.util.List;

public record AssistantResponse(String intent, String reply, List<String> suggestedQuestions) { }