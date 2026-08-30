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

/**
 * Rule-based scam / phishing scanner.
 *
 * Pipeline: input -> type detection -> indicator extraction -> risk scoring
 * -> risk level -> scam type -> explanation.
 *
 * A {@link TextRule} matches keywords. Each matched rule contributes one
 * indicator whose weight reflects severity. When a "decisive" scam phrase is
 * present the weight is raised, and when several keywords from the same rule
 * match a saturation bonus is added: real scams repeat their lures, genuine
 * messages usually do not.
 */
@Service
public class ScamAnalysisService {

    private static final Pattern LINK_FINDER = Pattern.compile(
            "(?i)\\bhttps?://[\\w.\\-@:/?#=~%&+()*!$'\"]+"
                    + "|\\bwww\\.[\\w.\\-@:/?#=~%&+()*!$'\"]+"
                    + "|\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}(?:/[\\w.\\-@:/?#=~%&+()*!$'\"]*)?");

    private static final int MAX_LINKS = 3;
    private static final int MAX_SCORE = 99;
    private static final int BASE_SCORE = 4;
    private static final int SCAN_CONTEXT = 1; // reserved for future ML fusion

    private static final List<TextRule> TEXT_RULES = List.of(
            new TextRule("OTP_HARVESTING", "OTP / PIN / password request", 26, 40,
                    List.of("share otp", "send otp", "send your otp", "share your otp", "your otp", "send us otp",
                            "verify otp", "enter otp", "submit otp", "type otp", "type the otp", "tell me otp",
                            "give otp", "otp to confirm", "otp to verify", "send otp to verify", "otp code", "upi pin",
                            "atm pin", "login pin", "netbanking password", "confirm otp", "otp will be",
                            "otp is valid", "share the otp"),
                    List.of("share otp", "send otp", "send your otp", "share your otp", "send us otp", "verify otp",
                            "enter otp", "type otp", "tell me otp", "give otp", "otp to confirm", "otp to verify",
                            "share your upi pin", "share your pin", "upi pin", "atm pin", "share the otp")),
            new TextRule("INFO_HARVESTING", "Requests private financial details", 26, 40,
                    List.of("aadhaar", "aadhar", "pan card", "pan number", "cvv", "card number", "card no",
                            "bank details", "account details", "debit card", "credit card", "date of birth",
                            "share your details", "share your bank", "netbanking", "screen recording", "screen share"),
                    List.of("aadhaar", "pan card", "cvv", "card number", "card no", "debit card", "credit card",
                            "share your details", "share your bank", "netbanking", "screen recording", "screen share")),
            new TextRule("KYC_SUSPENSION", "KYC / identity-verification request", 26, 42,
                    List.of("kyc", "update kyc", "verify kyc", "complete kyc", "kyc expired", "re-kyc",
                            "kyc verification", "kyc update", "kyc is incomplete", "incomplete kyc", "kyc needs",
                            "kyc required", "identity document", "submit aadhaar", "upload aadhaar"),
                    List.of("kyc expired", "re-kyc", "update kyc", "verify kyc", "complete kyc", "kyc is incomplete",
                            "incomplete kyc", "kyc update", "kyc verification")),
            new TextRule("ACCOUNT_SUSPENSION", "Account-blocking / suspension threat", 22, 36,
                    List.of("account will be blocked", "account will be suspended", "account suspended",
                            "will be suspended", "will be blocked", "block your account", "blocked today",
                            "blocked permanently", "deactivate your account", "suspend your account",
                            "limit your account", "account was blocked", "account closed", "account will be closed",
                            "permanent block"),
                    List.of("account will be blocked", "account will be suspended", "will be suspended",
                            "will be blocked", "blocked today", "blocked permanently", "deactivate your account",
                            "suspend your account", "account will be closed")),
            new TextRule("PRIZE_LOTTERY", "Prize, lottery or reward bait", 24, 40,
                    List.of("congratulations", "you won", "you have won", "winner", "prize", "lottery", "jackpot",
                            "lucky draw", "claim your prize", "claim your gift", "claim your winnings",
                            "winning amount", "you are selected", "award notification", "gift voucher", "free gift"),
                    List.of("you won", "you have won", "claim your prize", "claim your gift", "claim your winnings",
                            "winning amount", "lucky draw", "jackpot", "lottery")),
            new TextRule("INVESTMENT_BAIT", "Too-good-to-be-true investment", 26, 60,
                    List.of("guaranteed returns", "double your money", "triple your money", "doubles in 30 days",
                            "crypto", "bitcoin", "forex", "stock tips", "get rich", "no risk investment",
                            "risk free", "trading app", "daily profit", "guaranteed profit", "minimum investment",
                            "pump and dump", "pump", "turn 1000 into", "100% profit", "profit guarantee"),
                    List.of("guaranteed returns", "double your money", "triple your money", "doubles in 30 days",
                            "no risk investment", "risk free", "guaranteed profit", "100% profit",
                            "profit guarantee", "turn 1000 into")),
            new TextRule("REMOTE_ACCESS", "Wants remote access / app install", 24, 40,
                    List.of("anydesk", "teamviewer", "screen share", "remote access", "install this app",
                            "record your screen", "share screen", ".apk"),
                    List.of("install this app", "record your screen", "share screen", ".apk", "remote access")),
            new TextRule("UPFRONT_FEE", "Asks for an advance payment / fee", 20, 34,
                    List.of("processing fee", "registration fee", "joining fee", "advance fee", "to release",
                            "to claim your", "pay first", "token amount", "shipping fee", "customs fee",
                            "delivery fee", "activation fee", "verification fee", "small fee", "a fee"),
                    List.of("processing fee", "registration fee", "advance fee", "to claim your",
                            "pay first", "shipping fee", "customs fee", "delivery fee", "activation fee")),
            new TextRule("URGENCY", "Urgency — act immediately / last warning", 18, 26,
                    List.of("urgent", "immediately", "within 24 hours", "within 2 hours", "today only", "act now",
                            "expires soon", "last chance", "last warning", "limited time", "final reminder",
                            "as soon as possible", "immediate action", "don't wait", "urgently", "do it now"),
                    List.of("within 24 hours", "within 2 hours", "act now", "today only", "immediately",
                            "last warning", "urgent")),
            new TextRule("LOAN_SCAM", "Advance-fee loan offer", 20, 56,
                    List.of("loan approved", "instant loan", "pre-approved loan", "personal loan",
                            "sanctioned amount", "limited period loan"),
                    List.of("loan approved", "instant loan", "pre-approved loan", "sanctioned amount")),
            new TextRule("JOB_SCAM", "Fake job / work-from-home bait", 20, 56,
                    List.of("work from home", "part time", "earn daily", "earn money", "joining bonus",
                            "telegram job", "join our telegram", "data entry job", "profit daily", "lazy income",
                            "easy income", "earn 500", "earn 1000", "earn 5000", "earn rs", "earn ₹", "daily payout"),
                    List.of("work from home", "earn daily", "join our telegram", "telegram job", "data entry job",
                            "joining bonus", "earn 5000", "easy income")),
            new TextRule("FEAR_THREAT", "Threat or fear of losing money/access", 16, 30,
                    List.of("penalty", "legal action", "arrest", "case filed", "stop payment", "will be closed",
                            "permanent block", "blacklist", "fine", "charge a fee", "money will be deducted",
                            "deducted automatically"),
                    List.of("legal action", "arrest", "case filed", "stop payment", "money will be deducted")),
            new TextRule("SOCIAL_ENGINEERING", "Manipulative / emotional appeal", 16, 26,
                    List.of("western union", "moneygram", "help a friend", "emergency money", "my husband",
                            "girlfriend", "dating", "transfer to me", "my son", "my daughter", "accident",
                            "hospital bill", "send money now"),
                    List.of("western union", "moneygram", "emergency money", "transfer to me", "send money now")),
            new TextRule("AUTHORITY_IMPERSONATION", "Impersonation of a bank, brand or official", 12, 22,
                    List.of("bank", "rbi", "sbi", "hdfc", "icici", "axis", "kotak", "paytm", "phonepe",
                            "google pay", "gpay", "amazon", "flipkart", "irctc", "customer care", "support team",
                            "government", "income tax", "police", "bank official", "from the bank",
                            "ministry", "airtel", "jio", "vodafone"),
                    List.of("bank official", "from the bank", "income tax", "rbi")),
            new TextRule("DELIVERY_SCAM", "Parcel / delivery fee scam", 14, 26,
                    List.of("parcel", "delivery failed", "shipment", "customs", "courier", "package is held",
                            "undeliverable", "parcel is pending"),
                    List.of("delivery failed", "package is held", "undeliverable", "parcel is pending")),
            new TextRule("BILL_SCAM", "Utility bill disconnection threat", 14, 26,
                    List.of("electricity bill", "bill unpaid", "disconnection", "power cut", "water bill",
                            "reconnect", "bill overdue", "bill will be disconnected"),
                    List.of("disconnection", "power cut", "bill unpaid", "bill overdue", "bill will be disconnected")),
            new TextRule("REFUND_CASHBACK", "Refund / cashback bait", 12, 26,
                    List.of("refund", "cashback", "settlement", "money credited", "tax refund", "claim your refund",
                            "refund of", "refund amount"),
                    List.of("claim your refund", "tax refund", "refund amount")),
            new TextRule("PAYMENT_REQUEST", "Payment / UPI collect request", 12, 32,
                    List.of("upi collect", "payment request", "request money", "collect request", "approve payment",
                            "upi request", "requesting rs", "requesting ₹", "pay the amount", "pay now",
                            "approve the payment", "accept the request", "collect payment"),
                    List.of("upi collect", "upi request", "request money", "approve the payment",
                            "requesting rs", "requesting ₹", "collect request")));

    private static final Set<String> SHORTENERS = Set.of("bit.ly", "tinyurl", "bit.do", "t.co", "goo.gl", "is.gd",
            "buff.ly", "rb.gy", "cutt.ly", "shorturl.at", "ow.ly", "1url.com", "tny.im", "v.gd");
    private static final Set<String> SUSPECT_TLDS = Set.of("xyz", "top", "loan", "bond", "club", "gift", "click",
            "download", "zip", "tk", "ml", "ga", "cf", "icu", "work", "online", "site", "info", "biz", "vip", "win",
            "support", "accountants", "ru", "cn", "su", "cc", "rest", "cyou", "live", "pro", "ltd", "delivery",
            "shop", "store", "email", "today", "example", "invalid", "test");
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

    private static final Set<String> URL_THREAT_KINDS = Set.of("IP_ADDRESS", "SUSPECT_TLD", "PUNYCODE", "AT_SIGN_URL",
            "UNUSUAL_PORT", "MALWARE_EXTENSION", "BRAND_SQUATTING", "URL_SHORTENER");

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

        int rawScore = BASE_SCORE + indicators.stream().mapToInt(FraudIndicator::getWeight).sum();
        int riskScore = Math.min(MAX_SCORE, rawScore);
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
            List<String> matched = distinctEvidence(rule.keywords().stream()
                    .filter(keyword -> matchesPhrase(lower, keyword))
                    .toList());
            if (matched.isEmpty()) continue;
            boolean decisive = rule.decisive().stream().anyMatch(keyword -> matchesPhrase(lower, keyword));
            int weight = decisive ? rule.decisiveWeight() : rule.weight();
            if (matched.size() >= 2) {
                weight += Math.min(10, (matched.size() - 1) * 5);
            }
            indicators.add(new FraudIndicator(null, rule.kind(), rule.label(), weight));
            seenKinds.add(rule.kind());
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
        if (isShortenerHost(hostLower)) {
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

        String brandMatch = BRAND_DOMAINS.keySet().stream().filter(hostLower::contains).findFirst().orElse(null);
        boolean officialBrand = false;
        if (brandMatch != null) {
            String cleanHost = hostLower.replaceFirst("^www\\.", "");
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

        boolean hasStructuralRedFlag = indicators.stream().anyMatch(i -> URL_THREAT_KINDS.contains(i.getKind()));
        if (hasStructuralRedFlag && PHISHING_KEYWORDS.stream().anyMatch(lower::contains)) {
            indicators.add(new FraudIndicator(null, "PHISHING_KEYWORDS", "Contains words commonly used in phishing links (login/verify/reward/otp...).", 12));
        }

        return indicators;
    }

    private boolean isShortenerHost(String host) {
        String clean = host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        return SHORTENERS.stream().anyMatch(s -> clean.equals(s) || (!clean.contains("/") && clean.endsWith("." + s)));
    }

    private boolean decisiveBrandText(String lower) {
        return BRAND_DOMAINS.keySet().stream().anyMatch(brand -> lower.contains(brand)
                && (lower.contains(" login") || lower.contains(" verify") || lower.contains(" otp") || lower.contains(" reward")));
    }

    private String scamType(List<FraudIndicator> indicators, String raw) {
        Set<String> kinds = new LinkedHashSet<>();
        Map<String, Integer> kindMaxWeight = new java.util.HashMap<>();
        indicators.forEach(indicator -> kinds.add(indicator.getKind()));
        indicators.forEach(indicator -> kindMaxWeight.merge(indicator.getKind(), indicator.getWeight(), Math::max));

        if (kinds.contains("REMOTE_ACCESS")) return "Remote-Access / Screen-Share Scam";
        if (kinds.contains("KYC_SUSPENSION")) return "KYC / Account-Suspension Scam";
        if (kinds.contains("ACCOUNT_SUSPENSION") && kinds.contains("PAYMENT_REQUEST")) return "UPI / Payment-Request Scam";
        if (kinds.contains("OTP_HARVESTING") && kinds.contains("PAYMENT_REQUEST")) return "UPI / Payment-Request Scam";
        if (kinds.contains("INFO_HARVESTING") && kinds.contains("AUTHORITY_IMPERSONATION")) return "Phishing / Credential-Theft Scam";
        if (urlThreatSeverity(indicators) >= 30) return "Phishing / Credential-Theft Scam";
        if (kinds.contains("PRIZE_LOTTERY") && kinds.contains("UPFRONT_FEE")) return "Lottery / Advance-Fee Scam";
        if (kinds.contains("INVESTMENT_BAIT")) return "Investment Scam";
        if (kinds.contains("LOAN_SCAM")) return "Advance-Fee Loan Scam";
        if (kinds.contains("JOB_SCAM")) return "Job / Work-from-home Scam";
        if (kinds.contains("PRIZE_LOTTERY")) return "Lottery / Prize Scam";
        if (kinds.contains("DELIVERY_SCAM") && kinds.contains("UPFRONT_FEE")) return "Delivery / Parcel Scam";
        if (kinds.contains("OTP_HARVESTING")) return "OTP-Harvester Scam";
        if (kinds.contains("INFO_HARVESTING")) return "Identity-Information Scam";
        if (kinds.contains("ACCOUNT_SUSPENSION")) return "Account-Suspension Scam";
        if (kindMaxWeight.getOrDefault("BILL_SCAM", 0) >= 26) return "Utility-Bill Scam";
        if (kindMaxWeight.getOrDefault("DELIVERY_SCAM", 0) >= 26) return "Delivery / Parcel Scam";
        if (kinds.contains("SOCIAL_ENGINEERING")) return "Social-Engineering Scam";
        if (kindMaxWeight.getOrDefault("REFUND_CASHBACK", 0) >= 26) return "Refund / Cashback Scam";
        if (kinds.contains("FEAR_THREAT")) return "Intimidation / Threat Scam";

        if (kinds.contains("AUTHORITY_IMPERSONATION") && kinds.contains("PAYMENT_REQUEST")) {
            return "Impersonation / Payment-Request Scam";
        }

        if (!kinds.isEmpty()) {
            boolean onlyWeak = indicators.stream().allMatch(indicator -> indicator.getWeight() < 24);
            if (onlyWeak) return "Likely Safe";
            return "Phishing / Social-Engineering Scam";
        }
        return "Likely Safe";
    }

    private int urlThreatSeverity(List<FraudIndicator> indicators) {
        return indicators.stream()
                .filter(indicator -> URL_THREAT_KINDS.contains(indicator.getKind()))
                .mapToInt(FraudIndicator::getWeight).sum();
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
        if (score >= 50) return "Medium";
        return "Low";
    }

    /** Blueprint bands: 0–24 LOW, 25–49 MODERATE, 50–74 HIGH, 75–100 CRITICAL. */
    private String level(int score) {
        if (score <= 24) return "Low";
        if (score <= 49) return "Moderate";
        if (score <= 74) return "High";
        return "Critical";
    }

    private String buildSummary(FraudAnalysis.InputType type, String input, int score, String riskLabel,
                                String scamType, List<FraudIndicator> indicators) {
        StringBuilder summary = new StringBuilder();
        summary.append(riskLabel.toUpperCase(Locale.ROOT)).append(" — ").append(score).append("/100");
        if (type == FraudAnalysis.InputType.URL) {
            summary.append(" (URL scan). ");
        } else {
            summary.append(". ");
        }
        if (indicators.isEmpty()) {
            summary.append("No known scam patterns were detected. Treat unexpected messages with normal caution.");
        } else {
            summary.append("Possible type: ").append(scamType).append(". Why: ");
            summary.append(indicators.stream().map(FraudIndicator::getLabel).reduce((a, b) -> a + "; " + b).orElse(""));
            summary.append(". Do not share OTP, click unknown links, or transfer money until you verify through the official channel.");
        }
        return summary.toString();
    }

    /**
     * Matches a phrase in the message. Multi-word phrases use substring search.
     * Single tokens use word boundaries so "bank" does not fire on "banking".
     */
    private boolean matchesPhrase(String lower, String keyword) {
        String needle = keyword.toLowerCase(Locale.ROOT);
        if (needle.isBlank()) return false;
        if (needle.contains(" ") || needle.startsWith(".") || needle.length() <= 3) {
            return lower.contains(needle);
        }
        return Pattern.compile("\\b" + Pattern.quote(needle) + "\\b").matcher(lower).find();
    }

    /** Drop keywords that are already covered by a longer matched phrase (same evidence). */
    private List<String> distinctEvidence(List<String> matched) {
        return matched.stream()
                .filter(keyword -> matched.stream().noneMatch(other -> !other.equals(keyword) && other.contains(keyword)))
                .toList();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record TextRule(String kind, String label, int weight, int decisiveWeight,
                            List<String> keywords, List<String> decisive) { }
}