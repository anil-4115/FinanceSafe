# FinanceSafe — Problem & Solution (SIH)

## 1. The problem

Financial fraud is growing rapidly — KYC scams, OTP theft, phishing links,
lottery/prize fraud and impersonation are now among the most common ways
ordinary people lose money. The core problems victims face:

1. **They cannot tell a scam from a legitimate message.** Scams are designed
   to look urgent and official.
2. **Even when something is flagged, they don't know *why*.** Vague warnings
   create distrust and are ignored.
3. **They don't know how serious the risk is**, or what to do next.
4. **Fraud is behavioural too** — unusual transactions (e.g. an unexpected
   large transfer) can be the first sign of a compromise, yet there is no easy
   way to see how unusual an activity is.
5. **Financial data is sensitive.** Any credible solution must protect user
   data by design, yet many tools neglect basic security (IDOR leaks,
   insecure auth).

## 2. The proposed solution — FinanceSafe

**An explainable financial safety assistant** that detects, scores, explains
and protects — combining scam intelligence, behavioural anomaly detection, risk
scoring, security-health monitoring and actionable guidance in one secure
platform.

**Four questions FinanceSafe answers for the user:**

```text
1. Is this suspicious?        ->  Scam Scanner (rules + AI)
2. Why is it suspicious?      ->  Meaningful indicators & reasons
3. How serious is the risk?   ->  Risk score + severity (LOW→CRITICAL)
4. What should the user do?   ->  Clear recommended actions
```

## 3. How the platform addresses each problem

| Problem | FinanceSafe solution |
| ------- | -------------------- |
| Can't spot a scam | Hybrid rule-engine + learned-model detection over text and URLs |
| Don't know *why* | Every risk is explained with specific indicators and weights |
| Don't know severity | 0–100 risk score mapped to LOW/MODERATE/HIGH/CRITICAL |
| Don't know what to do | Actionable, human-readable recommendations |
| Behavioural fraud | Behavioural anomaly detection on the user's own transactions |
| Overall safety blind spot | A single Security/Financial Health Score with reasons |
| Sensitive data | JWT auth, per-user authorization, IDOR protection, safe errors |

## 4. Why it matters (social impact)

- Protects people who are most often targeted by KYC/OTP/impersonation scams.
- Educates users through the learning modules and quiz.
- Empowers confident, safer financial decision-making (budgets, goals, product
  comparison, what-if simulation all live in the same trusted app).
- Builds trust by **explaining** rather than merely warning.

## 5. The core philosophy

> **DETECT → EXPLAIN → SCORE → PROTECT**
