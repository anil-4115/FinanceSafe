package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.InvestmentRecommendationRequest;
import com.financialfraudassistant.dto.InvestmentRecommendationResponse;
import com.financialfraudassistant.model.FinancialProduct;
import com.financialfraudassistant.repository.FinancialProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InvestmentRecommendationService {

    private record AllocationSpec(String assetClass, int weight, List<String> categories, String guidance) { }

    private static final Map<String, int[]> PROFILES = Map.of(
            "LOW", new int[]{60, 25, 10, 5},
            "MODERATE", new int[]{30, 25, 35, 10},
            "HIGH", new int[]{10, 20, 60, 10},
            "AGGRESSIVE", new int[]{10, 10, 75, 5}
    );

    private static final List<AllocationSpec> SPECS = List.of(
            new AllocationSpec("Fixed income & deposits", 0, List.of("Fixed deposits", "Savings accounts", "Government schemes", "Recurring deposits"),
                    "Very low volatility; ideal for money you cannot afford to lose in the short term."),
            new AllocationSpec("Debt funds", 0, List.of("Debt funds"),
                    "Lower risk than equities; suitable for goals within 1–3 years."),
            new AllocationSpec("Equity (funds & index)", 0, List.of("Mutual funds", "ETFs"),
                    "Higher potential growth with higher short-term swings; best for goals 5+ years away."),
            new AllocationSpec("Gold", 0, List.of("Gold"),
                    "Acts as a hedge against inflation and market shocks; keep a small allocation.")
    );

    private final FinancialProductRepository productRepository;

    public InvestmentRecommendationService(FinancialProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public InvestmentRecommendationResponse recommend(InvestmentRecommendationRequest request) {
        BigDecimal amount = request.amount();
        int horizon = request.timeHorizonYears();
        String risk = normalizeRisk(request.riskTolerance());
        int[] weights = PROFILES.get(risk);
        if (weights == null) weights = PROFILES.get("MODERATE");

        int[] adjusted = adjustForHorizon(weights, horizon);
        List<InvestmentRecommendationResponse.Allocation> allocations = new ArrayList<>();
        for (int i = 0; i < SPECS.size(); i++) {
            AllocationSpec spec = SPECS.get(i);
            int weight = adjusted[i];
            BigDecimal allocationAmount = amount.multiply(BigDecimal.valueOf(weight)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            List<String> examples = productRepository.findByCategoryIn(spec.categories()).stream()
                    .map(FinancialProduct::getName).limit(3).toList();
            allocations.add(new InvestmentRecommendationResponse.Allocation(spec.assetClass(), weight, allocationAmount, spec.guidance(), examples));
        }

        String summary = "For a " + horizon + "-year horizon and a " + risk + " risk tolerance, a balanced plan spreads your " + amount.setScale(0) + " across " +
                allocations.stream().map(allocation -> allocation.assetClass().toLowerCase()).reduce((a, b) -> a + ", " + b).orElse("categories") + ".";

        return new InvestmentRecommendationResponse(risk, horizon, summary, allocations,
                "Educational guidance only. Real returns, taxes and fees vary. Consult a SEBI-registered investment advisor before investing.");
    }

    private int[] adjustForHorizon(int[] weights, int horizon) {
        int[] adjusted = weights.clone();
        if (horizon < 3) {
            int shift = adjusted[2] / 2;
            adjusted[2] -= shift;
            adjusted[0] += shift;
        }
        return adjusted;
    }

    private String normalizeRisk(String tolerance) {
        if (tolerance == null) return "MODERATE";
        return switch (tolerance.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "LOW", "CONSERVATIVE" -> "LOW";
            case "HIGH", "AGGRESSIVE" -> "AGGRESSIVE";
            case "MODERATE", "MEDIUM" -> "MODERATE";
            default -> "HIGH";
        };
    }
}