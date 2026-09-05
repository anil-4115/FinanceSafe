package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.InvestmentSimulationRequest;
import com.financialfraudassistant.dto.InvestmentSimulationResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvestmentSimulatorService {

    public InvestmentSimulationResponse simulate(InvestmentSimulationRequest request) {
        BigDecimal initial = value(request.initialInvestment());
        BigDecimal monthly = value(request.monthlyContribution());
        int years = Math.min(50, Math.max(1, request.years()));
        BigDecimal monthlyRate = request.annualReturnPct().divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        BigDecimal value = initial;
        BigDecimal contributed = initial;
        List<InvestmentSimulationResponse.YearPoint> series = new ArrayList<>();
        for (int year = 1; year <= years; year++) {
            for (int month = 0; month < 12; month++) {
                value = value.add(monthly).multiply(BigDecimal.ONE.add(monthlyRate));
                contributed = contributed.add(monthly);
            }
            series.add(new InvestmentSimulationResponse.YearPoint(year,
                    contributed.setScale(2, RoundingMode.HALF_UP),
                    value.setScale(2, RoundingMode.HALF_UP),
                    value.subtract(contributed).setScale(2, RoundingMode.HALF_UP)));
        }
        BigDecimal finalValue = value.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalContribution = contributed.setScale(2, RoundingMode.HALF_UP);
        return new InvestmentSimulationResponse(totalContribution, finalValue,
                finalValue.subtract(totalContribution), series,
                "This is a simulation using a fixed assumed return for illustration only. Real returns vary with market conditions, taxes and fees, and are never guaranteed.");
    }

    private static BigDecimal value(BigDecimal input) {
        return input == null ? BigDecimal.ZERO : input;
    }
}