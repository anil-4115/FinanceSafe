package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.MarketDetailResponse;
import com.financialfraudassistant.dto.MarketSearchResult;
import com.financialfraudassistant.model.Asset;
import com.financialfraudassistant.repository.AssetRepository;
import com.financialfraudassistant.repository.MarketPriceHistoryRepository;
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

    private static final int PERSISTED_HISTORY_WEEKS = 52;

    private final AssetRepository assetRepository;
    private final MarketPriceHistoryRepository marketPriceHistoryRepository;

    public MarketService(AssetRepository assetRepository, MarketPriceHistoryRepository marketPriceHistoryRepository) {
        this.assetRepository = assetRepository;
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
    }

    public List<MarketSearchResult> search(String query) {
        List<Asset> assets = universe();
        if (query == null || query.isBlank()) {
            return assets.stream().map(asset -> toResult(asset)).toList();
        }
        String lower = query.toLowerCase(Locale.ROOT);
        return assets.stream().filter(asset -> asset.getSymbol().toLowerCase(Locale.ROOT).contains(lower)
                        || asset.getName().toLowerCase(Locale.ROOT).contains(lower)
                        || asset.getSector().toLowerCase(Locale.ROOT).contains(lower))
                .map(asset -> toResult(asset)).toList();
    }

    public MarketDetailResponse detail(String symbol) {
        Asset asset = universe().stream().filter(item -> item.getSymbol().equalsIgnoreCase(symbol)).findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Symbol not found in the demo universe. Try RELIANCE, TCS, HDFCBANK, INFY, NIFTY50, etc."));
        return build(asset);
    }

    /**
     * Catalogue source of truth: the persisted {@code assets} table when it has
     * rows (seeded by DataSeeder), otherwise the built-in demo universe.
     */
    private List<Asset> universe() {
        if (assetRepository.count() > 0) {
            return assetRepository.findAllByOrderBySymbolAsc();
        }
        return builtinAssets();
    }

    private MarketDetailResponse build(Asset asset) {
        List<MarketDetailResponse.PricePoint> history = null;
        if (asset.getId() != null
                && marketPriceHistoryRepository.countByAssetId(asset.getId()) >= PERSISTED_HISTORY_WEEKS) {
            history = marketPriceHistoryRepository.findByAssetIdOrderByPriceDateAsc(asset.getId()).stream()
                    .map(point -> new MarketDetailResponse.PricePoint(point.getPriceDate().toString(), point.getPrice()))
                    .toList();
        }
        if (history == null) {
            history = generateWeeklyHistory(asset, LocalDate.now(), PERSISTED_HISTORY_WEEKS);
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

        BigDecimal volatility = annualizedVolatility(history);
        String riskLevel = volatility.doubleValue() < 12 ? "LOW" : volatility.doubleValue() < 20 ? "MEDIUM"
                : volatility.doubleValue() < 32 ? "HIGH" : "VERY HIGH";

        List<String> rationale = new ArrayList<>();
        rationale.add("The price trend over the last 12 months is " + trend.toLowerCase(Locale.ROOT) + ", comparing the recent quarter against the older period.");
        rationale.add("Annualised volatility of weekly moves is about " + volatility.setScale(1, RoundingMode.HALF_UP) + "%, which is " + riskLevel.toLowerCase(Locale.ROOT) + ".");
        rationale.add("Overall the symbol moved " + (changePct.signum() >= 0 ? "up" : "down") + " by " + changePct.abs().setScale(1, RoundingMode.HALF_UP) + "% over this illustrative period.");
        rationale.add("Past price behaviour does not predict future results. This is educational analysis, not advice to buy or sell.");

        return new MarketDetailResponse(asset.getSymbol(), asset.getName(), asset.getSector(), history, trend, volatility,
                riskLevel, changePct, rationale,
                "Illustrative price series generated deterministically for demonstration. Not a live market feed and not investment advice.");
    }

    /**
     * Deterministic weekly price series for an asset, used both as the built-in
     * display series and as the seed data written to the {@code market_price_history}
     * table.
     */
    public static List<MarketDetailResponse.PricePoint> generateWeeklyHistory(Asset asset, LocalDate end, int weeks) {
        Random random = new Random(asset.getSymbol().hashCode() * 31L + 2026L);
        List<MarketDetailResponse.PricePoint> history = new ArrayList<>();
        double price = asset.getBasePrice().doubleValue();
        double volatility = asset.getWeeklyVolatility().doubleValue();
        for (int i = weeks; i >= 0; i--) {
            LocalDate weekStart = end.minusDays(i * 7L);
            if (i > 0) {
                price = price * (1 + random.nextGaussian() * volatility);
            }
            history.add(new MarketDetailResponse.PricePoint(weekStart.toString(),
                    BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP)));
        }
        return history;
    }

    private BigDecimal annualizedVolatility(List<MarketDetailResponse.PricePoint> history) {
        if (history.size() < 3) return BigDecimal.ZERO;
        List<Double> weeklyReturns = new ArrayList<>();
        for (int i = 1; i < history.size(); i++) {
            double prev = history.get(i - 1).price().doubleValue();
            double curr = history.get(i).price().doubleValue();
            if (prev == 0) continue;
            weeklyReturns.add(curr / prev - 1);
        }
        if (weeklyReturns.size() < 2) return BigDecimal.ZERO;
        double mean = weeklyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = weeklyReturns.stream().mapToDouble(value -> Math.pow(value - mean, 2)).average().orElse(0);
        double std = Math.sqrt(variance);
        return BigDecimal.valueOf(std * Math.sqrt(52) * 100).setScale(1, RoundingMode.HALF_UP);
    }

    private MarketSearchResult toResult(Asset asset) {
        return new MarketSearchResult(asset.getSymbol(), asset.getName(), asset.getAssetType(), asset.getSector());
    }

    /**
     * The out-of-the-box demo catalogue, shared by the fallback universe and the
     * DataSeeder, so the two can never drift apart.
     */
    public static List<Asset> builtinAssets() {
        return List.of(
                asAsset("RELIANCE", "Reliance Industries Ltd", "STOCK", "Energy & Telecom", 2900, 0.022),
                asAsset("TCS", "Tata Consultancy Services Ltd", "STOCK", "IT Services", 4100, 0.02),
                asAsset("HDFCBANK", "HDFC Bank Ltd", "STOCK", "Banking", 1680, 0.018),
                asAsset("INFY", "Infosys Ltd", "STOCK", "IT Services", 1530, 0.024),
                asAsset("ICICIBANK", "ICICI Bank Ltd", "STOCK", "Banking", 1180, 0.02),
                asAsset("SBIN", "State Bank of India", "STOCK", "Banking", 780, 0.023),
                asAsset("ITC", "ITC Ltd", "STOCK", "FMCG", 440, 0.016),
                asAsset("TATAMOTORS", "Tata Motors Ltd", "STOCK", "Automobile", 980, 0.03),
                asAsset("ASIANPAINT", "Asian Paints Ltd", "STOCK", "Consumer Goods", 2900, 0.019),
                asAsset("NIFTY50", "Nifty 50 Index", "INDEX", "Broad Market Index", 24500, 0.012),
                asAsset("GOLDBEES", "Gold ETF", "ETF", "Gold", 68, 0.011),
                asAsset("NIFTYBANK", "Nifty Bank Index", "INDEX", "Banking Index", 52000, 0.017),
                asAsset("NIFTYCS", "Nifty Large Cap Index Fund", "MUTUAL_FUND", "Equity (Large Cap)", 108, 0.014),
                asAsset("BSE500", "BSE 500 Index", "INDEX", "Broad Market Index", 19000, 0.013));
    }

    private static Asset asAsset(String symbol, String name, String assetType, String sector, double basePrice, double weeklyVolatility) {
        return new Asset(symbol, name, assetType, sector,
                BigDecimal.valueOf(basePrice), BigDecimal.valueOf(weeklyVolatility));
    }
}