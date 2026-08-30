package com.financialfraudassistant.service;

import com.financialfraudassistant.model.FraudAnalysis;
import com.financialfraudassistant.model.FraudIndicator;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FraudAnalysisRepository;
import com.financialfraudassistant.repository.FraudIndicatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScamAnalysisServiceTest {

    private static final String KYC_SCAM = "Dear Customer, Your SBI account will be suspended within 24 hours due to " +
            "incomplete KYC. Update your KYC immediately at sbi-update-kyc.xyz to avoid permanent block. Share OTP to confirm.";
    private static final String GENUINE_SMS = "Your Flipkart order #12345 has been shipped. Track at flipkart.com. " +
            "Thank you for shopping with us.";
    private static final String GENUINE_URL = "https://www.flipkart.com/mobile-phones";
    private static final String SUSPICIOUS_URL = "https://sbi-secure-login-update.xyz/verify-account";
    private static final String GENUINE_BANK_ALERT = "SBI: A transaction of Rs. 5,000 has been made from your account " +
            "ending 1234. If not done by you, call our customer care.";

    @Mock private FraudAnalysisRepository analysisRepository;
    @Mock private FraudIndicatorRepository indicatorRepository;

    private ScamAnalysisService service;
    private final User user = new User("scanner@example.com", "hash", "Scanner Tester");

    @BeforeEach
    void setUp() {
        service = new ScamAnalysisService(analysisRepository, indicatorRepository);
        when(analysisRepository.save(any(FraudAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(indicatorRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private FraudAnalysis analyze(String content) {
        return service.analyze(user, content, null);
    }

    private List<FraudIndicator> analyzeIndicators(String content) {
        return analyze(content).getIndicators();
    }

    private boolean hasKind(String content, String kind) {
        return analyzeIndicators(content).stream().anyMatch(indicator -> indicator.getKind().equals(kind));
    }

    @Test
    void sihKycDemo_isCritical_withFivePlusIndicators() {
        String demo = "Your bank account will be blocked today.\nComplete KYC immediately:\n"
                + "http://suspicious-link.example\nSend your OTP to verify.";
        FraudAnalysis result = analyze(demo);
        assertEquals("Critical", result.getRiskLabel());
        assertTrue(result.getRiskScore() >= 75);
        assertTrue(result.getIndicators().size() >= 5);
        assertTrue(hasKind(demo, "KYC_SUSPENSION"));
        assertTrue(hasKind(demo, "OTP_HARVESTING"));
        assertTrue(hasKind(demo, "URGENCY"));
        assertTrue(hasKind(demo, "ACCOUNT_SUSPENSION"));
    }

    @Test
    void safeStatement_isLow() {
        FraudAnalysis result = analyze("Your monthly statement is ready. Please sign in using your normal banking application.");
        assertEquals("Low", result.getRiskLabel());
        assertEquals("Likely Safe", result.getScamType());
    }

    @Test
    void otpScam_isHighOrCritical() {
        FraudAnalysis result = analyze("This is HDFC Bank. Share the OTP immediately or your account will be blocked. Pay the verification fee.");
        assertTrue(result.getRiskScore() >= 50, "Expected HIGH/CRITICAL, got " + result.getRiskScore());
        assertTrue(hasKind("This is HDFC Bank. Share the OTP immediately or your account will be blocked. Pay the verification fee.",
                "OTP_HARVESTING"));
    }

    @Test
    void lotteryScam_isHighOrCritical() {
        FraudAnalysis result = analyze("Congratulations! You won a lottery prize. Claim your winnings after paying the processing fee.");
        assertTrue(result.getRiskScore() >= 50, "Expected HIGH/CRITICAL, got " + result.getRiskScore());
        assertEquals("Lottery / Advance-Fee Scam", result.getScamType());
    }

    @Test
    void impersonationScam_isHighOrCritical() {
        FraudAnalysis result = analyze("Income tax department: legal action today. Bank official requires your OTP and account details immediately.");
        assertTrue(result.getRiskScore() >= 50, "Expected HIGH/CRITICAL, got " + result.getRiskScore());
        assertTrue(hasKind("Income tax department: legal action today. Bank official requires your OTP and account details immediately.",
                "AUTHORITY_IMPERSONATION"));
    }

    @Test
    void kycScam_isCritical_andDetectsMultipleIndicators() {
        FraudAnalysis result = analyze(KYC_SCAM);
        assertEquals("Critical", result.getRiskLabel());
        assertEquals("KYC / Account-Suspension Scam", result.getScamType());
        assertTrue(result.getIndicators().size() >= 5);
        assertTrue(hasKind(KYC_SCAM, "KYC_SUSPENSION"));
        assertTrue(hasKind(KYC_SCAM, "OTP_HARVESTING"));
        assertTrue(hasKind(KYC_SCAM, "BRAND_SQUATTING"));
    }

    @Test
    void genuineFlipkartSms_isLow_andSafe() {
        FraudAnalysis result = analyze(GENUINE_SMS);
        assertEquals("Low", result.getRiskLabel());
        assertEquals("Likely Safe", result.getScamType());
        assertFalse(hasKind(GENUINE_SMS, "URL_SHORTENER"), "flipkart.com must not be flagged as a URL shortener");
    }

    @Test
    void upiScam_isClassifiedAsUpiPaymentRequest() {
        String content = "Rs. 9,999 UPI collect request. Approve the payment request or your account " +
                "will be blocked. Share your UPI PIN to reverse it.";
        FraudAnalysis result = analyze(content);
        assertEquals("UPI / Payment-Request Scam", result.getScamType());
        assertTrue(result.getRiskScore() >= 60);
        assertTrue(result.getIndicators().stream().anyMatch(indicator -> indicator.getKind().equals("PAYMENT_REQUEST")));
    }

    @Test
    void investmentScam_reachesHigh() {
        FraudAnalysis result = analyze("Double your money in 30 days with guaranteed returns. No risk investment.");
        assertTrue(result.getRiskScore() >= 61, "Expected HIGH, got " + result.getRiskScore());
        assertEquals("High", result.getRiskLabel());
        assertEquals("Investment Scam", result.getScamType());
    }

    @Test
    void jobScam_reachesHigh() {
        FraudAnalysis result = analyze("Work from home job available. Earn Rs. 5000 daily on WhatsApp. Join our Telegram group.");
        assertTrue(result.getRiskScore() >= 61, "Expected HIGH, got " + result.getRiskScore());
        assertEquals("Job / Work-from-home Scam", result.getScamType());
    }

    @Test
    void genuineUrl_isLow_andSafe() {
        FraudAnalysis result = analyze(GENUINE_URL);
        assertEquals("Low", result.getRiskLabel());
        assertEquals("Likely Safe", result.getScamType());
    }

    @Test
    void suspiciousUrl_isPhishing() {
        FraudAnalysis result = analyze(SUSPICIOUS_URL);
        assertTrue(result.getRiskScore() >= 60);
        assertEquals("Phishing / Credential-Theft Scam", result.getScamType());
        assertTrue(hasKind(SUSPICIOUS_URL, "BRAND_SQUATTING"));
        assertTrue(hasKind(SUSPICIOUS_URL, "SUSPECT_TLD"));
    }

    @Test
    void genuineBankAlert_isLow() {
        FraudAnalysis result = analyze(GENUINE_BANK_ALERT);
        String indicatorDump = result.getIndicators().stream()
                .map(indicator -> indicator.getKind() + "=" + indicator.getWeight())
                .reduce((a, b) -> a + ", " + b).orElse("none");
        assertEquals("Low", result.getRiskLabel());
        assertEquals("Likely Safe", result.getScamType(),
                "kinds/weights were: " + indicatorDump);
        assertTrue(result.getIndicators().stream().allMatch(indicator -> indicator.getWeight() < 24),
                "all indicators should be weak, got: " + indicatorDump);
    }

    @Test
    void plainMessage_hasNoIndicators() {
        FraudAnalysis result = analyze("How are you? See you at dinner tonight.");
        assertEquals("Low", result.getRiskLabel());
        assertTrue(result.getIndicators().isEmpty());
    }

    @Test
    void plainUrlIsTreatsAsUrlInput() {
        FraudAnalysis result = analyze(SUSPICIOUS_URL);
        assertEquals(FraudAnalysis.InputType.URL, result.getInputType());
    }
}