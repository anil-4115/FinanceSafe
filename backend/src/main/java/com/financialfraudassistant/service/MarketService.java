package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.MarketDetailResponse;
import com.financialfraudassistant.dto.MarketSearchResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
public class MarketService {

    private record Asset(String symbol, String name, String assetType, String sector, double basePrice, double weeklyVolatility) { }

    private static final List<Asset> UNIVERSE = List.of(
            new Asset("RELIANCE", "Reliance Industries Ltd", "STOCK", "Energy & Telecom", 2900, 0.022),
            new Asset("TCS", "Tata Consultancy Services Ltd", "STOCK", "IT Services", 4100, 0.02),
            new Asset("HDFCBANK", "HDFC Bank Ltd", "STOCK", "Banking", 1680, 0.018),
            new Asset("INFY", "Infosys Ltd", "STOCK", "IT Services", 1530, 0.024),
            new Asset("ICICIBANK", "ICICI Bank Ltd", "STOCK", "Banking", 1180, 0.02),
            new Asset("SBIN", "State Bank of India", "STOCK", "Banking", 780, 0.023),
            new Asset("ITC", "ITC Ltd", "STOCK", "FMCG", 440, 0.016),
            new Asset("TATAMOTORS", "Tata Motors Ltd", "STOCK", "Automobile", 980, 0.03),
            new Asset("ASIANPAINT", "Asian Paints Ltd", "STOCK", "Consumer Goods", 2900, 0.019),
            new Asset("NIFTY50", "Nifty 50 Index", "INDEX", "Broad Market Index", 24500, 0.012),
            new Asset("GOLDBEES", "Gold ETF", "ETF", "Gold", 68, 0.011),
            new Asset("NIFTYBANK", "Nifty Bank Index", "INDEX", "Banking Index", 52000, 0.017),
            new Asset("NIFTYCS", "Nifty Large Cap Index Fund", "MUTUAL_FUND", "Equity (Large Cap)", 108, 0.014),
            new Asset("BSE500", "BSE 500 Index", "INDEX", "Broad Market Index", 19000, 0.013)
    );

    public List<MarketSearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return UNIVERSE.stream().map(asset -> toResult(asset)).toList();
        }
        String lower = query.toLowerCase(Locale.ROOT);
        return UNIVERSE.stream().filter(asset -> asset.symbol().toLowerCase(Locale.ROOT).contains(lower)
                        || asset.name().toLowerCase(Locale.ROOT).contains(lower)
                        || asset.sector().toLowerCase(Locale.ROOT).contains(lower))
                .map(asset -> toResult(asset)).toList();
    }

    public MarketDetailResponse detail(String symbol) {
        Asset asset = UNIVERSE.stream().filter(item -> item.symbol().equalsIgnoreCase(symbol)).findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Symbol not found in the demo universe. Try RELIANCE, TCS, HDFCBANK, INFY, NIFTY50, etc."));
        return build(asset);
    }

    private MarketDetailResponse build(Asset asset) {
        Random random = new Random(asset.symbol().hashCode() * 31L + 2026L);
        List<MarketDetailResponse.PricePoint> history = new ArrayList<>();
        LocalDate end = LocalDate.now();
        double price = asset.basePrice();
        List<Double> returns = new ArrayList<>();
        for (int i = 51; i >= 0; i--) {
            LocalDate weekStart = end.minusDays(i * 7L);
            if (i > 0) {
                double returnPct = random.nextGaussian() * asset.weeklyVolatility();
                returns.add(returnPct);
                price = price * (1 + returnPct);
            }
            history.add(new MarketDetailResponse.PricePoint(weekStart.toString(),
                    BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP)));
        }

        BigDecimal startPrice = history.get(0).price();
        BigDecimal lastPrice = history.get(history.size() - 1).price();
        BigDecimal changePct = startPrice.signum() == 0 ? BigDecimal.ZERO
                : lastPrice.subtract(startPrice).multiply(BigDecimal.valueOf(100)).divide(startPrice, 2, RoundingMode.HALF_UP);

        double avgRecent = history.subList(history.size() - 12, history.size()).stream()
                .mapToDouble(point -> point.price().doubleValue()).average().orElse(0);
        double avgOlder = history.subList(0, 12).stream()
                .mapToDouble(point -> point.price().doubleValue()).average().orElse(0);
        String trend = avgRecent > avgOlder * 1.01 ? "Positive" : avgRecent < avgOlder * 0.99 ? "Negative" : "Neutral";

        BigDecimal volatility = annualizedVolatility(returns);
        String riskLevel = volatility.doubleValue() < 12 ? "LOW" : volatility.doubleValue() < 20 ? "MEDIUM"
                : volatility.doubleValue() < 32 ? "HIGH" : "VERY HIGH";

        List<String> rationale = new ArrayList<>();
        rationale.add("The price trend over the last 12 months is " + trend.toLowerCase(Locale.ROOT) + ", comparing the recent quarter against the older period.");
        rationale.add("Annualised volatility of weekly moves is about " + volatility.setScale(1, RoundingMode.HALF_UP) + "%, which is " + riskLevel.toLowerCase(Locale.ROOT) + ".");
        rationale.add("Overall the symbol moved " + (changePct.signum() >= 0 ? "up" : "down") + " by " + changePct.abs().setScale(1, RoundingMode.HALF_UP) + "% over this illustrative period.");
        rationale.add("Past price behaviour does not predict future results. This is educational analysis, not advice to buy or sell.");

        return new MarketDetailResponse(asset.symbol(), asset.name(), asset.sector(), history, trend, volatility,
                riskLevel, changePct, rationale,
                "Illustrative price series generated deterministically for demonstration. Not a live market feed and not investment advice.");
    }

    private BigDecimal annualizedVolatility(List<Double> weeklyReturns) {
        if (weeklyReturns.size() < 2) return BigDecimal.ZERO;
        double mean = weeklyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = weeklyReturns.stream().mapToDouble(value -> Math.pow(value - mean, 2)).average().orElse(0);
        double std = Math.sqrt(variance);
        return BigDecimal.valueOf(std * Math.sqrt(52) * 100).setScale(1, RoundingMode.HALF_UP);
    }

    private MarketSearchResult toResult(Asset asset) {
        return new MarketSearchResult(asset.symbol(), asset.name(), asset.assetType(), asset.sector());
    }
}