package com.financialfraudassistant.dto;

public record DemoDataResponse(String message, int transactions, int budgets, int goals, int alerts, boolean alreadyLoaded) { }