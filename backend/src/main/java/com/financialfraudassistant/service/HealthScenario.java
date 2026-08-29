package com.financialfraudassistant.service;

import java.math.BigDecimal;

public class HealthScenario {
    private BigDecimal monthlyIncomeDelta = BigDecimal.ZERO;
    private BigDecimal monthlyExpenseDelta = BigDecimal.ZERO;
    private BigDecimal oneTimeSpend = BigDecimal.ZERO;
    private BigDecimal savingsBoost = BigDecimal.ZERO;
    private BigDecimal extraDebt = BigDecimal.ZERO;

    public BigDecimal getMonthlyIncomeDelta() { return monthlyIncomeDelta; }
    public void setMonthlyIncomeDelta(BigDecimal value) { this.monthlyIncomeDelta = value == null ? BigDecimal.ZERO : value; }
    public BigDecimal getMonthlyExpenseDelta() { return monthlyExpenseDelta; }
    public void setMonthlyExpenseDelta(BigDecimal value) { this.monthlyExpenseDelta = value == null ? BigDecimal.ZERO : value; }
    public BigDecimal getOneTimeSpend() { return oneTimeSpend; }
    public void setOneTimeSpend(BigDecimal value) { this.oneTimeSpend = value == null ? BigDecimal.ZERO : value; }
    public BigDecimal getSavingsBoost() { return savingsBoost; }
    public void setSavingsBoost(BigDecimal value) { this.savingsBoost = value == null ? BigDecimal.ZERO : value; }
    public BigDecimal getExtraDebt() { return extraDebt; }
    public void setExtraDebt(BigDecimal value) { this.extraDebt = value == null ? BigDecimal.ZERO : value; }
}