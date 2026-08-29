package com.financialfraudassistant.service;

import com.financialfraudassistant.model.FraudAnalysis;
import com.financialfraudassistant.model.FraudIndicator;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FraudAnalysisRepository;
import com.financialfraudassistant.repository.FraudIndicatorRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScamAnalysisService {

    private static final Pattern LINK_FINDER = Pattern.compile(
            "(?i)\\bhttps?://[\\w.\\-@:/?#=~%&+()*!$'\"]+"
                    + "|\\bwww\\.[\\w.\\-@:/?#=~%&+()*!$'\"]+"
                    + "|\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}(?:/[\\w.\\-@:/?#=~%&+()*!$'\"]*)?");

    private static final int MAX_LINKS = 3;

    private static final List<TextRule> TEXT_RULES = List.of(
            new TextRule("OTP_HARVESTING", "Request for OTP / PIN / password", 26,
                    List.of("share otp", "send otp", "send us otp", "verify otp", "enter otp", "submit otp",
                            "type otp", "type the otp", "tell me otp", "give otp", "otp to confirm", "otp code",
                            "upi pin", "atm pin", "login pin", "password", "netbanking password", "confirm otp")),
            new TextRule("INFO_HARVESTING", "Requests private financial details", 26,
                    List.of("aadhaar", "aadhar", "pan card", "pan number", "cvv", "card number", "card no",
                            "bank details", "account details", "debit card", "credit card", "date of birth",
                            "share your details", "share your bank", "netbanking", "screen recording", "screen share")),
            new TextRule("KYC_SUSPENSION", "KYC / account-suspension pressure", 26,
                    List.of("kyc", "update kyc", "verify kyc", "kyc expired", "re-kyc", "account will be blocked",
                            "account will be suspended", "account suspended", "will be suspended", "will be blocked",
                            "block your account", "blocked permanently", "deactivate your account", "suspend your account",
                            "limit your account", "account was blocked", "deactivated")),
            new TextRule("PRIZE_LOTTERY", "Prize, lottery or reward bait", 24,
                    List.of("congratulations", "you won", "you have won", "winner", "prize", "lottery", "jackpot",
                            "lucky draw", "claim your prize", "claim your gift", "claim your winnings", "winning amount",
                            "you are selected", "award notification")),
            new TextRule("INVESTMENT_BAIT", "Too-good-to-be-true investment", 24,
                    List.of("guaranteed returns", "double your money", "triple your money", "doubles in 30 days",
                            "crypto", "bitcoin", "forex", "stock tips", "get rich", "no risk investment",
                            "risk free", "trading app", "daily profit", "guaranteed profit", "minimum investment",
                            "pump and dump", "pump")),
            new TextRule("REMOTE_ACCESS", "Wants remote access / app install", 24,
                    List.of("anydesk", "teamviewer", "screen share", "remote access", "install this app",
                            "record your screen", "share screen", ".apk")),
            new TextRule("UPFRONT_FEE", "Asks for an advance payment / fee", 20,
                    List.of("processing fee", "registration fee", "joining fee", "advance fee", "to release",
                            "to claim your", "pay first", "token amount", "shipping fee", "customs fee",
                            "delivery fee", "activation fee", "verification fee")),
            new TextRule("URGENCY", "Urgency — pushes you to act immediately", 18,
                    List.of("urgent", "immediately", "within 24 hours", "within 2 hours", "today only", "act now",
                            "expires soon", "last chance", "limited time", "final reminder", "as soon as possible",
                            "immediate action", "don't wait", "urgently")),
            new TextRule("LOAN_SCAM", "Advance-fee loan offer", 18,
                    List.of("loan approved", "instant loan", "pre-approved loan", "personal loan",
                            "sanctioned amount", "limited period loan")),
            new TextRule("JOB_SCAM", "Fake job / work-from-home bait", 18,
                    List.of("work from home", "part time", "earn daily", "earn money", "joining bonus",
                            "telegram job", "data entry job", "profit daily", "lazy income", "easy income")),
            new TextRule("FEAR_THREAT", "Threat or fear of losing money/access", 16,
                    List.of("penalty", "legal action", "arrest", "case filed", "stop payment", "will be closed",
                            "permanent block", "blacklist", "fine", "charge a fee")),
            new TextRule("SOCIAL_ENGINEERING", "Manipulative / emotional appeal", 16,
                    List.of("western union", "moneygram", "help a friend", "emergency money", "my husband",
                            "girlfriend", "dating", "transfer to me", "my son", "my daughter", "accident")),
            new TextRule("AUTHORITY_IMPERSONATION", "Impersonates a trusted organisation", 12,
                    List.of("bank", "rbi", "sbi", "hdfc", "icici", "axis", "kotak", "paytm", "phonepe",
                            "google pay", "gpay", "amazon", "flipkart", "irctc", "customer care", "support team",
                            "government", "income tax", "police", "telecom", "airtel", "jio", "vodafone")),
            new TextRule("DELIVERY_SCAM", "Parcel / delivery fee scam", 14,
                    List.of("parcel", "delivery failed", "shipment", "customs", "courier", "package is held",
                            "undeliverable", "parcel is pending")),
            new TextRule("BILL_SCAM", "Utility bill disconnection threat", 14,
                    List.of("electricity bill", "bill unpaid", "disconnection", "power cut", "water bill",
                            "reconnect", "bill overdue")),
            new TextRule("REFUND_CASHBACK", "Refund / cashback bait", 12,
                    List.of("refund", "cashback", "settlement", "money credited", "tax refund", "claim your refund")),
            new TextRule("PAYMENT_REQUEST", "Payment / UPI collect request", 12,
                    List.of("upi collect", "payment request", "request money", "collect request", "approve payment",
                            "upi request", "requesting rs", "requesting ₹", "pay the amount"))
    );

    private static final Set<String> SHORTENERS = Set.of("bit.ly", "tinyurl", "bit.do", "t.co", "goo.gl", "is.gd",
            "buff.ly", "rb.gy", "cutt.ly", "shorturl.at", "ow.ly", "1url.com");
    private static final Set<String> LOW_SIGNAL_KINDS = Set.of("AUTHORITY_IMPERSONATION", "REFUND_CASHBACK",
            "PAYMENT_REQUEST", "BILL_SCAM", "DELIVERY_SCAM");
    private static final Set<String> SUSPECT_TLDS = Set.of("xyz", "top", "loan", "bond", "club", "gift", "click",
            "download", "zip", "tk", "ml", "ga", "cf", "icu", "work", "online", "site", "info", "biz", "vip", "win",
            "support", "accountants", "ru", "cn", "su", "cc", "rest", "cyou", "live", "pro", "ltd", "delivery",
            "shop", "store", "email", "today");
    private static final Map<String, List<String>> BRAND_DOMAINS = Map.ofEntries(
            Map.entry("paytm", List.of("paytm.com")),
            Map.entry("phonepe", List.of("phonepe.com", "phonepe.in")),
            Map.entry("gpay", List.of("pay.google.com", "googlepay.google.com")),
            Map.entry("googlepay", List.of("pay.google.com")),
            Map.entry("sbi", List.of("sbi.co.in", "onlinesbi.sbi", "sbi.com", "sbi.in")),
            Map.entry("hdfc", List.of("hdfcbank.com", "hdfc.com")),
            Map.entry("icici", List.of("icicibank.com", "icici.com")),
            Map.entry("axis", List.of("axisbank.com", "axisb.com")),
            Map.entry("kotak", List.of("kotak.com", "kotakbank.com")),
            Map.entry("amazon", List.of("amazon.in", "amazon.com")),
            Map.entry("flipkart", List.of("flipkart.com")),
            Map.entry("irctc", List.of("irctc.co.in")),
            Map.entry("uber", List.of("uber.com")),
            Map.entry("ola", List.of("olacabs.com", "olamoney.com")),
            Map.entry("airtel", List.of("airtel.in", "airtel.com")),
            Map.entry("jio", List.of("jio.com")));

    private static final Set<String> PHISHING_KEYWORDS = Set.of("login", "verify", "secure", "update", "account",
            "wallet", "otp", "redeem", "reward", "gift", "cashback", "coupon", "free", "token", "recharge", "bonus",
            "kyc", "claim", "unlock", "activate");

    private final FraudAnalysisRepository analysisRepository;
    private final FraudIndicatorRepository indicatorRepository;

    public ScamAnalysisService(FraudAnalysisRepository analysisRepository, FraudIndicatorRepository indicatorRepository) {
        this.analysisRepository = analysisRepository;
        this.indicatorRepository = indicatorRepository;
    }

    public FraudAnalysis analyze(User user, String content, String requestedType) {
        String clean = content.trim();
        FraudAnalysis.InputType type = resolveType(clean, requestedType);
        List<FraudIndicator> indicators = type == FraudAnalysis.InputType.URL
                ? analyzeUrl(clean)
                : analyzeMessage(clean);

        int riskScore = Math.min(99, 6 + indicators.stream().mapToInt(FraudIndicator::getWeight).sum());
        String riskLabel = level(riskScore);
        String scamType = scamType(indicators, clean);
        String confidence = confidence(riskScore, indicators);
        String summary = buildSummary(type, clean, riskScore, riskLabel, scamType, indicators);

        FraudAnalysis analysis = analysisRepository.save(new FraudAnalysis(user, type, truncate(clean, 9999), riskScore, riskLabel, scamType, confidence, summary));
        List<FraudIndicator> saved = indicatorRepository.saveAll(indicators.stream().map(indicator -> new FraudIndicator(analysis, indicator.getKind(), indicator.getLabel(), indicator.getWeight())).toList());
        analysis.getIndicators().addAll(saved);
        return analysis;
    }

    private List<FraudIndicator> analyzeMessage(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<FraudIndicator> indicators = new ArrayList<>();
        Set<String> seenKinds = new LinkedHashSet<>();

        for (TextRule rule : TEXT_RULES) {
            if (seenKinds.contains(rule.kind())) continue;
            if (rule.keywords().stream().anyMatch(lower::contains)) {
                indicators.add(new FraudIndicator(null, rule.kind(), rule.label(), rule.weight()));
                seenKinds.add(rule.kind());
            }
        }

        int linksChecked = 0;
        Matcher matcher = LINK_FINDER.matcher(text);
        while (matcher.find() && linksChecked < MAX_LINKS) {
            String link = matcher.group().trim();
            if (link.endsWith(".") || link.endsWith(",") || link.endsWith("?") || link.endsWith("!")
                    || link.endsWith("\"") || link.endsWith("'") || link.endsWith(")")) {
                link = link.substring(0, link.length() - 1);
            }
            for (FraudIndicator urlIndicator : analyzeUrl(link)) {
                if (seenKinds.add(urlIndicator.getKind())) indicators.add(urlIndicator);
            }
            linksChecked++;
        }
        return indicators;
    }

    private List<FraudIndicator> analyzeUrl(String value) {
        List<FraudIndicator> indicators = new ArrayList<>();
        String lower = value.toLowerCase(Locale.ROOT);
        String url = value.trim();
        if (!url.matches("(?i)^https?://.*")) url = "https://" + url;
        String host = "";
        try {
            URI uri = new URI(url);
            host = uri.getHost() == null ? "" : uri.getHost();
        } catch (URISyntaxException ignored) {
            host = lower.replace("https://", "").replace("http://", "").split("/")[0];
        }
        String hostLower = host.toLowerCase(Locale.ROOT);

        if (hostLower.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            indicators.add(new FraudIndicator(null, "IP_ADDRESS", "URL points to a raw IP address instead of a real domain.", 22));
        }

        String domain = hostLower.replaceFirst("^www\\.", "");
        String tld = domain.contains(".") ? domain.substring(domain.lastIndexOf('.') + 1) : "";
        if (SUSPECT_TLDS.contains(tld)) {
            indicators.add(new FraudIndicator(null, "SUSPECT_TLD", "Unusual top-level domain: ." + tld + " is rarely used by genuine banks or services.", 20));
        }
        if (SHORTENERS.stream().anyMatch(hostLower::contains)) {
            indicators.add(new FraudIndicator(null, "URL_SHORTENER", "Redirect/URL-shortener hides the final destination address.", 16));
        }
        if (hostLower.contains("xn--")) {
            indicators.add(new FraudIndicator(null, "PUNYCODE", "Internationalised (punycode) domain used to mimic a trusted name.", 18));
        }
        if (hostLower.contains("@")) {
            indicators.add(new FraudIndicator(null, "AT_SIGN_URL", "URL contains '@', a common trick to hide the real destination.", 18));
        }
        if (lower.matches("(?i).*:\\d{4,}.*")) {
            indicators.add(new FraudIndicator(null, "UNUSUAL_PORT", "Link uses a non-standard port number.", 14));
        }
        if (lower.contains(".apk") || lower.contains(".exe") || lower.contains(".zip")) {
            indicators.add(new FraudIndicator(null, "MALWARE_EXTENSION", "Link points to an installable/untrusted file type.", 18));
        }
        if (lower.startsWith("http://")) {
            indicators.add(new FraudIndicator(null, "WEAK_PROTOCOL", "Link uses plain HTTP instead of a secure HTTPS connection.", 6));
        }

        String brandMatch = BRAND_DOMAINS.keySet().stream().filter(brand -> {
            String clean = hostLower.replace("www.", "");
            return clean.contains(brand);
        }).findFirst().orElse(null);
        boolean officialBrand = false;
        if (brandMatch != null) {
            String cleanHost = hostLower;
            for (String official : BRAND_DOMAINS.get(brandMatch)) {
                if (cleanHost.equals(official) || cleanHost.endsWith("." + official)) {
                    officialBrand = true;
                    break;
                }
            }
            if (!officialBrand) {
                indicators.add(new FraudIndicator(null, "BRAND_SQUATTING", "Domain contains '" + brandMatch + "' but is not the official website.", 22));
            }
        } else if (decisiveBrandText(lower)) {
            indicators.add(new FraudIndicator(null, "BRAND_MENTION", "Message name-drops a well-known brand inside a link context.", 8));
        }

        if (hostLower.chars().filter(ch -> ch == '-').count() >= 3) {
            indicators.add(new FraudIndicator(null, "HYPHEN_HEAVY", "Domain uses many hyphens, uncommon for official services.", 8));
        }
        long digits = hostLower.chars().filter(Character::isDigit).count();
        if (digits >= 5) {
            indicators.add(new FraudIndicator(null, "DIGIT_HEAVY", "Domain contains many random digits.", 6));
        }

        boolean hasStructuralRedFlag = !indicators.isEmpty();
        if (hasStructuralRedFlag && PHISHING_KEYWORDS.stream().anyMatch(lower::contains)) {
            indicators.add(new FraudIndicator(null, "PHISHING_KEYWORDS", "Contains words commonly used in phishing links (login/verify/reward/otp...).", 12));
        }

        return indicators;
    }

    private boolean decisiveBrandText(String lower) {
        return BRAND_DOMAINS.keySet().stream().anyMatch(brand -> lower.contains(brand)
                && (lower.contains(" login") || lower.contains(" verify") || lower.contains(" otp") || lower.contains(" reward")));
    }

    private String scamType(List<FraudIndicator> indicators, String raw) {
        Set<String> kinds = new LinkedHashSet<>();
        indicators.forEach(indicator -> kinds.add(indicator.getKind()));
        String lower = raw.toLowerCase(Locale.ROOT);
        boolean onlyLowSignal = true;
        for (String kind : kinds) {
            if (!LOW_SIGNAL_KINDS.contains(kind)) {
                onlyLowSignal = false;
                break;
            }
        }
        if (onlyLowSignal) return "Likely Safe";
        if (kinds.contains("REMOTE_ACCESS")) return "Remote-Access / Screen-Share Scam";
        if (kinds.contains("KYC_SUSPENSION")) return "KYC / Account-Suspension Scam";
        if (kinds.contains("INFO_HARVESTING") && kinds.contains("AUTHORITY_IMPERSONATION")) return "Phishing / Credential-Theft Scam";
        if (kinds.contains("PRIZE_LOTTERY") && kinds.contains("UPFRONT_FEE")) return "Lottery / Advance-Fee Scam";
        if (kinds.contains("INVESTMENT_BAIT")) return "Investment Scam";
        if (kinds.contains("JOB_SCAM")) return "Job / Work-from-home Scam";
        if (kinds.contains("PRIZE_LOTTERY")) return "Lottery / Prize Scam";
        if (kinds.contains("LOAN_SCAM")) return "Advance-Fee Loan Scam";
        if (kinds.contains("DELIVERY_SCAM")) return "Delivery / Parcel Scam";
        if (kinds.contains("BILL_SCAM")) return "Utility-Bill Scam";
        if (kinds.contains("SOCIAL_ENGINEERING")) return "Social-Engineering Scam";
        if (kinds.contains("REFUND_CASHBACK")) return "Refund / Cashback Scam";
        if (kinds.contains("OTP_HARVESTING") && kinds.contains("PAYMENT_REQUEST")) return "UPI / Payment-Request Scam";
        if (kinds.contains("OTP_HARVESTING")) return "OTP-Harvester Scam";
        if (kinds.contains("INFO_HARVESTING")) return "Identity-Information Scam";
        if (kinds.contains("FEAR_THREAT") && kinds.contains("AUTHORITY_IMPERSONATION")) return "Bank Impersonation / Account-Suspension Scam";
        if (kinds.contains("AUTHORITY_IMPERSONATION")) return "Fake Customer Support / Impersonation";
        if (lower.contains("upi://") || lower.contains("upi@")) return "UPI / Payment-Request Scam";
        if (lower.contains("://") && kinds.contains("BRAND_SQUATTING")) return "Phishing / Credential-Theft Scam";
        if (!kinds.isEmpty()) return "Phishing / Social-Engineering Scam";
        return "Likely Safe";
    }

    private FraudAnalysis.InputType resolveType(String content, String requestedType) {
        if (requestedType != null && !requestedType.isBlank()) {
            String requested = requestedType.trim().toLowerCase(Locale.ROOT);
            if (requested.equals("url") || requested.equals("link")) return FraudAnalysis.InputType.URL;
            return FraudAnalysis.InputType.TEXT;
        }
        String trimmed = content.trim();
        if (trimmed.matches("(?i)^https?://\\S+$") || trimmed.matches("(?i)^www\\.\\S+$")
                || trimmed.matches("(?i)^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.[a-z]{2,}(?:/.*)?$")) {
            return FraudAnalysis.InputType.URL;
        }
        return FraudAnalysis.InputType.TEXT;
    }

    private String confidence(int score, List<FraudIndicator> indicators) {
        if (score >= 75 && indicators.size() >= 2) return "High";
        if (score >= 40) return "Medium";
        return "Low";
    }

    private String level(int score) {
        if (score <= 40) return "Low";
        if (score <= 60) return "Moderate";
        if (score <= 80) return "High";
        return "Critical";
    }

    private String buildSummary(FraudAnalysis.InputType type, String input, int score, String riskLabel,
                                String scamType, List<FraudIndicator> indicators) {
        StringBuilder summary = new StringBuilder();
        String preview = input.length() > 180 ? input.substring(0, 180) + "…" : input;
        summary.append("Analysed ").append(type == FraudAnalysis.InputType.URL ? "a URL" : "a message").append(": \"").append(preview).append("\".\n");
        summary.append("The AI risk engine assigned a score of ").append(score).append("/100 (").append(riskLabel).append("). ");
        if (indicators.isEmpty()) {
            summary.append("No known scam patterns were detected. Treat it with normal caution, but remember scams constantly evolve.");
        } else {
            summary.append("Possible scam type: ").append(scamType).append(". ");
            summary.append("Detected ").append(indicators.size()).append(" signalling tactic(s): ")
                    .append(indicators.stream().map(indicator -> indicator.getLabel().replace(" — ", ": ")).toList().toString().toLowerCase(Locale.ROOT)).append(".");
        }
        summary.append("\nNote: this is an AI-generated risk assessment for awareness; it is not an official bank or law-enforcement determination.");
        return summary.toString();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record TextRule(String kind, String label, int weight, List<String> keywords) { }
}