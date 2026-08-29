package com.financialfraudassistant.service;
import com.financialfraudassistant.dto.ScamReportRequest;
import com.financialfraudassistant.model.Alert;
import com.financialfraudassistant.model.FinancialTransaction;
import com.financialfraudassistant.model.FraudAnalysis;
import com.financialfraudassistant.model.ScamReport;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.AlertRepository;
import com.financialfraudassistant.repository.FinancialTransactionRepository;
import com.financialfraudassistant.repository.ScamReportRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FraudDetectionService {
    private static final Set<String> HIGH_RISK_TERMS = Set.of("crypto", "gift card", "gambling", "betting", "upi collect", "wire transfer", "investment scheme");
    private final FinancialTransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final ScamReportRepository reportRepository;
    private final AnomalyService anomalyService;
    private final ScamAnalysisService scamAnalysisService;
    public FraudDetectionService(FinancialTransactionRepository transactionRepository, AlertRepository alertRepository, ScamReportRepository reportRepository, AnomalyService anomalyService, ScamAnalysisService scamAnalysisService) { this.transactionRepository = transactionRepository; this.alertRepository = alertRepository; this.reportRepository = reportRepository; this.anomalyService = anomalyService; this.scamAnalysisService = scamAnalysisService; }

    public void analyseTransaction(FinancialTransaction transaction) {
        boolean riskyAlerted = false;
        if (transaction.getTransactionType() == FinancialTransaction.Type.EXPENSE) {
            AnomalyService.Result anomaly = anomalyService.assess(transaction.getUser(), transaction.getId(),
                    transaction.getMerchant(), transaction.getAmount(), transaction.getCategory(), transaction.getTransactionDate());
            transaction.applyRisk(anomaly.score(), anomaly.level(), String.join(" ", anomaly.reasons()));
            transactionRepository.save(transaction);
            if (anomaly.score() >= 70) {
                create(transaction.getUser(), "Suspicious transaction detected",
                        "A " + transaction.getAmount().toPlainString() + " expense to " + transaction.getMerchant() + " looks unusual for you. " + String.join(" ", anomaly.reasons()),
                        anomaly.level().equals("CRITICAL") ? Alert.Severity.CRITICAL : Alert.Severity.WARNING,
                        "TRANSACTION_ANOMALY", anomaly.score());
                riskyAlerted = true;
            }
            analyseRuleEngine(transaction);
        }
    }

    private void analyseRuleEngine(FinancialTransaction transaction) {
        List<FinancialTransaction> history = transactionRepository.findByUserIdOrderByTransactionDateDescIdDesc(transaction.getUser().getId());
        List<FinancialTransaction> previousExpenses = history.stream().filter(item -> !item.getId().equals(transaction.getId()) && item.getTransactionType() == FinancialTransaction.Type.EXPENSE).toList();
        BigDecimal average = previousExpenses.isEmpty() ? BigDecimal.ZERO : previousExpenses.stream().map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(previousExpenses.size()), 2, RoundingMode.HALF_UP);
        boolean unusualAmount = transaction.getAmount().compareTo(BigDecimal.valueOf(5000)) >= 0 && (average.signum() == 0 || transaction.getAmount().compareTo(average.multiply(BigDecimal.valueOf(3))) > 0);
        boolean newMerchant = previousExpenses.stream().noneMatch(item -> item.getMerchant().equalsIgnoreCase(transaction.getMerchant()));
        String searchable = (transaction.getMerchant() + " " + transaction.getCategory()).toLowerCase(Locale.ROOT);
        boolean highRisk = HIGH_RISK_TERMS.stream().anyMatch(searchable::contains);
        if (unusualAmount) create(transaction.getUser(), "Unusually large payment", "This " + transaction.getAmount().toPlainString() + " expense is much higher than your previous spending pattern.", Alert.Severity.CRITICAL, "UNUSUAL_AMOUNT", 82);
        if (newMerchant && transaction.getAmount().compareTo(BigDecimal.valueOf(5000)) >= 0) create(transaction.getUser(), "New merchant needs review", "A high-value payment was recorded for a merchant not seen in your earlier expense history: " + transaction.getMerchant() + ".", Alert.Severity.WARNING, "NEW_MERCHANT", 62);
        if (highRisk) create(transaction.getUser(), "High-risk payment category", "This transaction mentions a category often used in financial scams. Confirm the payee and never share your OTP or UPI PIN.", Alert.Severity.WARNING, "HIGH_RISK_CATEGORY", 70);
        long sameDayExpenses = history.stream().filter(item -> item.getTransactionType() == FinancialTransaction.Type.EXPENSE && item.getTransactionDate().equals(transaction.getTransactionDate())).count();
        if (transaction.getSource() == FinancialTransaction.Source.MANUAL && sameDayExpenses >= 4) create(transaction.getUser(), "Rapid transaction activity", "Four or more expenses were recorded on the same day. Review them if you did not authorise each payment.", Alert.Severity.WARNING, "RAPID_ACTIVITY", 68);
    }

    public int analyseScamReport(User user, ScamReportRequest request) {
        String text = (request.channel() + " " + request.description());
        FraudAnalysis analysis = scamAnalysisService.analyze(user, text, "TEXT");
        int riskScore = analysis.getRiskScore();
        if (request.amountAtRisk() != null && request.amountAtRisk().compareTo(BigDecimal.valueOf(10000)) >= 0) {
            riskScore = Math.min(98, riskScore + 6);
        }
        reportRepository.save(new ScamReport(user, request.channel().trim(), request.description().trim(), request.amountAtRisk() == null ? BigDecimal.ZERO : request.amountAtRisk(), riskScore));
        Alert.Severity severity = riskScore >= 70 ? Alert.Severity.CRITICAL : Alert.Severity.WARNING;
        create(user, "Potential scam reported", "Your " + request.channel().trim() + " report has been recorded. " + analysis.getSummary() + " Contact your bank immediately if money was sent.", severity, "SCAM_REPORT", riskScore);
        return riskScore;
    }
    private void create(User user, String title, String message, Alert.Severity severity, String type, int score) { alertRepository.save(new Alert(user, title, message, severity, type, score)); }
}
