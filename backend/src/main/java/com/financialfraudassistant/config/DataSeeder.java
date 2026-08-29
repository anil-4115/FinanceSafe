package com.financialfraudassistant.config;

import com.financialfraudassistant.model.EducationModule;
import com.financialfraudassistant.model.FinancialProduct;
import com.financialfraudassistant.model.QuizQuestion;
import com.financialfraudassistant.repository.EducationModuleRepository;
import com.financialfraudassistant.repository.FinancialProductRepository;
import com.financialfraudassistant.repository.QuizQuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final FinancialProductRepository productRepository;
    private final EducationModuleRepository moduleRepository;
    private final QuizQuestionRepository questionRepository;

    public DataSeeder(FinancialProductRepository productRepository, EducationModuleRepository moduleRepository,
                      QuizQuestionRepository questionRepository) {
        this.productRepository = productRepository;
        this.moduleRepository = moduleRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() == 0) seedProducts();
        if (moduleRepository.count() == 0) seedEducation();
    }

    private void seedProducts() {
        productRepository.saveAll(List.of(
                new FinancialProduct("Savings account", "Savings accounts", "Very Low", "3-4% p.a.", "High", "0", "Any", "Everyone",
                        "Liquid and insured", "Very low interest",
                        "You can access your money immediately. Ideal for daily needs, though interest is low."),
                new FinancialProduct("Fixed deposit", "Fixed deposits", "Low", "6-7.5% p.a.", "Low (lock-in)", "500", "Months to years", "Capital-safe savers",
                        "Guaranteed returns", "Penalty on early withdrawal",
                        "Fixed interest with minimal risk. Choose tenure and rate at your bank; premature withdrawal may attract charges."),
                new FinancialProduct("Recurring deposit", "Recurring deposits", "Low", "6-7% p.a.", "Medium", "100", "Months to years", "Disciplined savers",
                        "Compounding every month", "Less flexible if you skip payments",
                        "Save a fixed amount every month and earn interest like a fixed deposit. Good for disciplined savers."),
                new FinancialProduct("Public Provident Fund", "Government schemes", "Very Low", "About 7.1% p.a.", "Low (15-year lock-in)", "500", "15 years", "Long-term tax savers",
                        "Tax-free under 80C and EEE", "Long lock-in",
                        "Backed by the government with tax benefits under Section 80C. Long lock-in but very safe."),
                new FinancialProduct("NSC (National Savings Certificate)", "Government schemes", "Very Low", "About 6.7% p.a.", "Low", "1000", "5 years", "Medium-term savers",
                        "Government-backed", "Interest is taxable",
                        "Government-backed certificate with fixed returns and tax benefits under 80C. Suited for medium-term goals."),
                new FinancialProduct("Liquid mutual fund", "Debt funds", "Low-Moderate", "4-6% p.a.", "High", "500", "Days to weeks", "Anyone parking emergency money",
                        "Easy exit with low volatility", "Returns fluctuate daily",
                        "Low-risk debt fund with easy exits. Suitable for parking emergency money."),
                new FinancialProduct("Short-term debt fund", "Debt funds", "Moderate", "5-7% p.a.", "Medium", "500", "Months", "Conservative investors",
                        "Better than savings accounts", "Affected by interest-rate moves",
                        "Invests in short-duration bonds. Generally stable returns above savings accounts."),
                new FinancialProduct("Government securities bond", "Debt funds", "Low", "6-7% p.a.", "Medium", "1000", "Years", "Steady-income investors",
                        "Very low default risk", "Bond prices move with rates",
                        "Government bonds held to maturity give steady interest with very low default risk."),
                new FinancialProduct("Nifty 50 index fund", "Mutual funds", "Moderate-High", "10-12% p.a.*", "High", "500", "Years", "Long-term equity investors",
                        "Low cost, instant diversification", "Rides market ups and downs",
                        "Passively tracks the top 50 companies on the NSE. Cheap and diversified equity exposure."),
                new FinancialProduct("Large-cap mutual fund", "Mutual funds", "Moderate-High", "10-13% p.a.*", "High", "500", "Years", "Growth-focused investors",
                        "Professional management and diversification", "Active fees apply",
                        "Active equity fund investing in large, established companies. Good for long-term growth."),
                new FinancialProduct("Mid-cap mutual fund", "Mutual funds", "High", "12-15% p.a.*", "High", "1000", "Years", "Risk-tolerant investors",
                        "Higher growth potential", "Higher volatility than large caps",
                        "Focuses on midsized companies. Higher growth potential with higher volatility."),
                new FinancialProduct("SIP in ELSS", "Mutual funds", "High", "10-14% p.a.*", "Low (3-year lock-in)", "500", "3 years (SIP)", "Tax-saving investors",
                        "80C deduction up to 1.5 lakh", "Locks money for 3 years",
                        "Equity-linked savings scheme offering tax deduction under 80C with a 3-year lock-in."),
                new FinancialProduct("Gold ETF", "Gold", "Low-Moderate", "Tracks gold prices", "High", "100", "Any", "Inflation hedgers",
                        "Pure electronic gold", "No interest; demat account needed",
                        "Tracks the price of gold electronically. A classic hedge against inflation."),
                new FinancialProduct("Sovereign Gold Bond", "Gold", "Low", "Gold + 2.5% interest", "Low (8-year lock-in)", "1000", "8 years", "Long-term gold investors",
                        "Pays interest on gold holding", "Long lock-in",
                        "Government-backed gold investment paying fixed interest. Redeem at gold-linked value on maturity."),
                new FinancialProduct("Indian equity ETF", "ETFs", "High", "10-13% p.a.*", "High", "100", "Any", "Cost-conscious investors",
                        "Trades like a stock with low fees", "Requires a demat account",
                        "Exchange-traded fund giving instant diversification across an index with low expense ratio."),
                new FinancialProduct("Nifty Bank ETF", "ETFs", "High", "10-15% p.a.*", "High", "100", "Any", "Experienced investors",
                        "Focused sector exposure", "Sector concentration risk",
                        "Tracks banking stocks. Can swing widely; suitable for experienced investors.")
        ));
    }

    private void seedEducation() {
        EducationModule upi = saveModule("UPI safety in 5 minutes", "upi-safety", "Fraud awareness",
                "UPI is safe when you follow simple rules:\n\n1. NEVER share your UPI PIN, OTP or UPI number with anyone - not even bank staff.\n2. Only approve a collect request you actually initiated.\n3. Ignore 'free money' or 'safe do not share' pressure messages.\n4. Verify the payee handle / VPA before paying.\n5. If you get a collect request you did not ask for, reject it and report the VPA at cybercrime.gov.in.\n\nRemember: a genuine bank NEVER asks for your PIN or OTP on a call or SMS.",
                4, 1);
        saveQuestion(upi, "What should you NEVER share with anyone?", new String[]{"UPI PIN", "Name", "Bank branch"}, 0, "UPI PIN and OTP are confidential; No one legitimate asks for them.");
        saveQuestion(upi, "A collect request arrives from a stranger. What do you do?", new String[]{"Approve it", "Reject it and report", "Share OTP to verify"}, 1, "Only approve collect requests you have initiated yourself.");
        saveQuestion(upi, "Who can legitimately ask for your UPI PIN?", new String[]{"Your bank manager", "RBI officials", "No one"}, 2, "Never share your PIN - never, with anyone.");

        EducationModule phishing = saveModule("Spotting phishing and fake links", "phishing", "Fraud awareness",
                "Phishing is a message pretending to be from a real company, sent to steal your PIN, OTP or money.\n\nRed flags:\n- Urgency: 'Your account will be BLOCKED today'.\n- Threats or rewards: 'You've won a lottery'.\n- Weird links: check the domain, not just the text.\n- Brand impersonation: look out for look-alike spelling of a brand name.\n- Asking for sensitive info over SMS or WhatsApp.\n\nWhen in doubt, open the app directly instead of clicking a link. Any suspicious SMS can be pasted into the FinanceSafe Scam Scanner for an instant analysis.",
                5, 2);
        saveQuestion(phishing, "Which text is a classic phishing red flag?", new String[]{"'Your account will be blocked if you don't share your OTP NOW.'", "'Your bill is ready, view it in the app.'", "'New features released in the update.'"}, 0, "Urgency plus a request for OTP/PIN equals phishing.");
        saveQuestion(phishing, "A message says 'lottery winners get cash gift'. It asks for a small fee. This is:", new String[]{"A promotional offer", "An advance-fee scam", "A lucky break"}, 1, "Free-money offers that ask for fees or OTP are scams.");
        saveQuestion(phishing, "The best reaction to a suspicious link in an SMS is:", new String[]{"Click to check", "Scan it in the Scam Scanner first", "Forward it to friends"}, 1, "Always verify before clicking. Forwarding can spread the scam.");

        EducationModule cardFraud = saveModule("Card fraud and safe online payments", "card-fraud", "Fraud awareness",
                "Card fraud takes many forms: skimming, online card theft, and 'card blocked' phishing.\n\nProtection checklist:\n- Turn on SMS/email alerts for every card transaction.\n- Never read out the CVV; keep it on the card.\n- Use virtual cards or a low-limit card online.\n- Shop only on HTTPS sites (padlock icon) and trusted apps.\n- Check statements every month for unknown charges and raise a dispute at once.",
                4, 3);
        saveQuestion(cardFraud, "Which is a safe online shopping habit?", new String[]{"Shopping only on HTTPS sites", "Sharing the CVV over WhatsApp", "Using the same PIN everywhere"}, 0, "HTTPS encrypts your payment data; never share CVV over chat.");
        saveQuestion(cardFraud, "Someone messages 'Your card is blocked, share the OTP to unblock'. What should you do?", new String[]{"Share OTP", "Check with your bank through the official app", "Mail your PIN"}, 1, "Banks never ask for OTP. Verify through official channels only.");

        EducationModule investing = saveModule("Investing basics for beginners", "investing-basics", "Safe investing",
                "Investing grows money over time. Key terms:\n\n- Compounding: returns earn their own returns. Starting early matters a lot.\n- Asset classes: equity (stocks/funds), debt (bonds/FD), gold, cash.\n- Diversification: spreading money reduces the risk of any single option failing.\n- Risk vs horizon: long horizons can ride out volatility; short goals need safer options.\n- Expense ratio: the annual fee of a fund; lower usually means more kept by you.\n\nThe FinanceSafe tool recommends an allocation based on your age, risk tolerance and goal timeline - use it before deciding.",
                6, 4);
        saveQuestion(investing, "Compounding means:", new String[]{"Returns earn further returns", "Fees compounding annually", "Interest doubling yearly"}, 0, "Compounding is returns growing on previous returns.");
        saveQuestion(investing, "For a goal 2 years away, this is the most appropriate home for the money:", new String[]{"High-risk stocks", "Debt and fixed income", "A single hot stock"}, 1, "Short horizons favour safer options like debt funds and deposits.");
        saveQuestion(investing, "Diversification mainly:", new String[]{"Increases risk", "Reduces the impact of a single loss", "Guarantees profit"}, 1, "Spreading across assets reduces single-point risk; it does not guarantee profit.");

        EducationModule hygiene = saveModule("Banking safety habits", "banking-hygiene", "Fraud awareness",
                "Small habits create strong protection:\n\n1. Strong unique password for net banking and email.\n2. Two-factor authentication enabled everywhere.\n3. Log in only through official apps or the bank website.\n4. Never use public Wi-Fi for banking.\n5. Set transaction and merchant limits on cards.\n6. Review notifications and statements regularly.\n7. Block lost cards immediately via the bank app.",
                3, 5);
        saveQuestion(hygiene, "Which habit reduces fraud risk the most?", new String[]{"Saving passwords in a public computer", "Using the same password for all sites", "Enabling two-factor authentication"}, 2, "2FA stops most unauthorized logins even if a password leaks.");
        saveQuestion(hygiene, "Public Wi-Fi for net banking is:", new String[]{"Risky - avoid", "Fine if fast", "Only okay at night"}, 0, "Public Wi-Fi makes it easy for attackers on the same network.");
    }

    private EducationModule saveModule(String title, String topic, String category, String content, int durationMins, int orderIndex) {
        return moduleRepository.save(new EducationModule(title, topic, category, content, durationMins, orderIndex));
    }

    private void saveQuestion(EducationModule module, String question, String[] options, int correctIndex, String explanation) {
        questionRepository.save(new QuizQuestion(module, question, String.join("\n", options), correctIndex, explanation));
    }
}