package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.IntelligenceResponse;
import com.financialfraudassistant.model.FraudAnalysis;
import com.financialfraudassistant.model.ScamReport;
import com.financialfraudassistant.repository.FraudAnalysisRepository;
import com.financialfraudassistant.repository.ScamReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "ML/AI Analysis" stage of the fraud-intelligence pipeline.
 *
 * <p>A small naive-Bayes style model learned from the application's own data:
 * confirmed high-severity fraud analyses plus community scam reports as the
 * "scam" corpus, low/moderate-risk analyses as the "benign" corpus. Every token
 * gets a Laplace-smoothed log-odds weight, and an input is scored by summing
 * the weights of the tokens it contains before a logistic transform. The result
 * is an explainable, data-driven risk estimate used to corroborate rule-engine
 * signals at the "ml/ai analysis" pipeline stage.</p>
 */
@Service
public class FraudIntelligenceService {

    /** Prior probability a scanned input is a scam when the corpus is balanced. */
    private static final double BASE_RATE = 0.2;
    /** Laplace smoothing count. */
    private static final double ALPHA = 1.0;
    private static final double WEIGHT_CLAMP = 6.0;
    private static final int MIN_TOKEN_LENGTH = 4;

    public static final int NO_DATA = -1;

    private static final Set<String> STOPWORDS = Set.of(
            "your", "this", "that", "with", "have", "will", "from", "they", "their", "there",
            "into", "were", "been", "only", "more", "most", "some", "such", "them", "then",
            "just", "about", "would", "could", "should", "these", "those", "please", "click",
            "after", "thank", "thanks", "dear", "customer", "message", "received", "today");

    private final FraudAnalysisRepository analysisRepository;
    private final ScamReportRepository scamReportRepository;

    private Map<String, Double> tokenWeights = Map.of();
    private List<String> topGlobalSignals = List.of();
    private long scamDocs;
    private long benignDocs;
    private long vocabSize;
    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    private LocalDateTime modelBuiltAt = EPOCH;
    private boolean enabled = true;
    private long lastScamCount = -1;
    private long lastBenignCount = -1;

    public FraudIntelligenceService(FraudAnalysisRepository analysisRepository, ScamReportRepository scamReportRepository) {
        this.analysisRepository = analysisRepository;
        this.scamReportRepository = scamReportRepository;
    }

    public static FraudIntelligenceService disabled() {
        FraudIntelligenceService service = new FraudIntelligenceService(null, null);
        service.enabled = false;
        return service;
    }

    /**
     * Returns an AI risk estimate in 0..99 for the given content, or
     * {@link #NO_DATA} when the model has no training corpus yet.
     */
    public int estimate(String text) {
        if (!enabled) return NO_DATA;
        Model model = currentModel();
        if (model.scamDocs() + model.benignDocs() == 0) return NO_DATA;
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) return NO_DATA;
        double logOdds = Math.log(BASE_RATE / (1 - BASE_RATE));
        for (String token : tokens) {
            Double weight = model.weights().get(token);
            if (weight != null) logOdds += weight;
        }
        double probability = 1.0 / (1.0 + Math.exp(-clamp(logOdds, -50, 50)));
        return (int) Math.round(clamp(probability * 99, 1, 99));
    }

    /** Tokens that contributed positively to a risk estimate, strongest first. */
    public List<String> topSignals(String text) {
        if (!enabled) return List.of();
        Model model = currentModel();
        double prior = Math.log(BASE_RATE / (1 - BASE_RATE));
        return tokenize(text).stream()
                .distinct()
                .map(token -> Map.entry(token, model.weights().getOrDefault(token, 0.0)))
                .filter(entry -> entry.getValue() > 0.05)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(6)
                .map(entry -> entry.getKey() + " (+" + Math.round(entry.getValue() * 10) / 10.0 + ")")
                .toList();
    }

    public IntelligenceResponse metadata() {
        Model model = currentModel();
        List<String> stages = List.of(
                "User Input", "Input Classification", "Indicator / Feature Extraction",
                "Rule Engine", "ML/AI Analysis", "Risk Score", "Risk Level",
                "Explanation", "Recommended Action");
        return new IntelligenceResponse(stages, 0.7, 0.3, BASE_RATE,
                model.scamDocs(), model.benignDocs(), model.weights().size(), model.builtAt(),
                new ArrayList<>(topGlobalSignals));
    }

    private synchronized Model currentModel() {
        if (!enabled) return new Model(Map.of(), 0, 0, EPOCH);
        long scamCount = countScamCorpus();
        long benignCount = countBenignCorpus();
        if (tokenWeights.isEmpty() || scamCount != lastScamCount || benignCount != lastBenignCount) {
            rebuild(scamCount, benignCount);
        }
        return new Model(tokenWeights, scamDocs, benignDocs, modelBuiltAt);
    }

    private void rebuild(long scamCount, long benignCount) {
        Map<String, Long> scamCounts = new HashMap<>();
        Map<String, Long> benignCounts = new HashMap<>();
        long scamTotal = 0;
        long benignTotal = 0;

        for (ScamReport report : scamReportRepository.findAll()) {
            for (String token : tokenize(report.getDescription())) {
                scamCounts.merge(token, 1L, Long::sum);
                scamTotal += 1;
            }
        }
        for (FraudAnalysis analysis : analysisRepository.findByRiskLabelIn(List.of("High", "Critical"))) {
            for (String token : tokenize(analysis.getInput())) {
                scamCounts.merge(token, 1L, Long::sum);
                scamTotal += 1;
            }
        }
        for (FraudAnalysis analysis : analysisRepository.findByRiskLabelIn(List.of("Low", "Moderate"))) {
            for (String token : tokenize(analysis.getInput())) {
                benignCounts.merge(token, 1L, Long::sum);
                benignTotal += 1;
            }
        }

        Map<String, Double> weights = new HashMap<>();
        Set<String> vocab = new HashSet<>(scamCounts.size() + benignCounts.size());
        vocab.addAll(scamCounts.keySet());
        vocab.addAll(benignCounts.keySet());
        double denomScam = scamTotal + ALPHA * vocab.size();
        double denomBenign = benignTotal + ALPHA * vocab.size();
        for (String token : vocab) {
            double pScam = (scamCounts.getOrDefault(token, 0L) + ALPHA) / denomScam;
            double pBenign = (benignCounts.getOrDefault(token, 0L) + ALPHA) / denomBenign;
            double weight = Math.log(pScam / pBenign);
            weights.put(token, clamp(weight, -WEIGHT_CLAMP, WEIGHT_CLAMP));
        }

        this.tokenWeights = weights;
        this.scamDocs = scamCount;
        this.benignDocs = benignCount;
        this.vocabSize = vocab.size();
        this.modelBuiltAt = LocalDateTime.now();
        this.lastScamCount = scamCount;
        this.lastBenignCount = benignCount;
        this.topGlobalSignals = wordsThatCoOccurInScams(weights, scamCounts);
    }

    private List<String> wordsThatCoOccurInScams(Map<String, Double> weights, Map<String, Long> scamCounts) {
        return scamCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 1 && weights.getOrDefault(entry.getKey(), 0.0) > 0.1)
                .sorted((a, b) -> Double.compare(weights.getOrDefault(b.getKey(), 0.0), weights.getOrDefault(a.getKey(), 0.0)))
                .limit(10)
                .map(entry -> entry.getKey() + " (+" + Math.round(weights.get(entry.getKey()) * 10) / 10.0 + ")")
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private long countScamCorpus() {
        return scamReportRepository.count() + analysisRepository.countByRiskLabelIn(List.of("High", "Critical"));
    }

    private long countBenignCorpus() {
        return analysisRepository.countByRiskLabelIn(List.of("Low", "Moderate"));
    }

    List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) return tokens;
        String cleaned = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ");
        for (String token : cleaned.split("\\s+")) {
            if (token.length() < MIN_TOKEN_LENGTH) continue;
            if (token.chars().allMatch(Character::isDigit)) continue;
            if (STOPWORDS.contains(token)) continue;
            tokens.add(token);
        }
        return tokens;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Model(Map<String, Double> weights, long scamDocs, long benignDocs, LocalDateTime builtAt) { }
}