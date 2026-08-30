# FinanceSafe — AI/ML Documentation

FinanceSafe uses a **hybrid fraud-intelligence** approach: a deterministic
rule engine plus a learned (naive-Bayes) model. This couples the speed and
explainability of rules with a genuine data-driven classifier, all running
inside the Spring Boot backend (no separate Python service for the MVP).

---

## 1. Hybrid pipeline

```text
             Input
               |
      +--------+--------+
      |                 |
      v                 v
  Rule Engine       ML/AI Analysis
  (ScamAnalysis)    (FraudIntelligence)
      |                 |
      +--------+--------+
               |
               v
        Risk Aggregator
               |
        +------+------+
        |             |
        v             v
    Risk Score    Explanation
```

- **Rules** → fast, deterministic, explainable for known scam patterns.
- **Learned model** → captures subtle token-level signals across many scans
  and community reports, and generalises beyond fixed rules.

## 2. The ML stage — `FraudIntelligenceService`

A small **naive-Bayes** text classifier learned from the application's own
data:

- **Scam corpus:** confirmed `High`/`Critical` fraud analyses **plus**
  community scam reports (`scam_reports`).
- **Benign corpus:** `Low`/`Moderate` fraud analyses.

### Model training

Every token in the corpus receives a **Laplace-smoothed** log-odds weight:

```text
weight(token) = ln( P(token | scam) / P(token | benign) )
```

with:

- smoothing `ALPHA = 1.0`
- weight clamp `±6.0`
- prior/base rate `BASE_RATE = 0.2`
- minimum token length `4` with a small stop-word list
- all-numeric tokens ignored

### Scoring an input

```text
logOdds = ln( BASE_RATE / (1 - BASE_RATE) )
        + sum of weights of input tokens present in the model
probability = 1 / (1 + e^(-clamp(logOdds)))     (logistic transform)
estimate    = round( clamp(probability * 99, 1, 99) )
```

When the model has no training corpus yet, `estimate` returns `NO_DATA = -1`.

### Explainable output

`topSignals(content)` returns the tokens that contributed **positively** to
the risk estimate, strongest first (up to 6), e.g.

```json
["otp (+2.1)", "blocked (+1.8)", "verify (+1.2)"]
```

This gives the "why" for the AI signal, consistent with the project's
**DETECT → EXPLAIN → SCORE → PROTECT** philosophy.

### Rebuild behaviour

The model is rebuilt lazily and synchronised whenever the underlying corpus
counts change, so it stays current with new scans/reports.

## 3. The rule engine — `ScamAnalysisService`

A deterministic rules engine with ~29 text rules plus URL analysis covering the
blueprint's scam categories:

- KYC scams (update request, account-blocking threat, credential request)
- OTP / verification-code requests ("share the OTP")
- Impersonation (fake bank / government / customer-care)
- Urgency ("act now", "account will be blocked", "last warning")
- Lottery / prize scams (winner, processing fee, advance payment)
- Suspicious payments (unknown beneficiary, payment before verification)
- Suspicious URLs (lookalike / unknown domains, credential-collection links)

Each matched rule contributes **meaningful evidence** (kind + label + weight).
Duplicates of the same evidence are not double-counted, so scores reflect real
signals, not inflated counts.

### Risk thresholds

| Score | Severity |
| ----- | -------- |
| 0–24 | LOW |
| 25–49 | MODERATE |
| 50–74 | HIGH |
| 75–100 | CRITICAL |

## 4. Behavioural anomaly detection — `AnomalyService`

Anomaly detection identifies transactions that deviate from a user's own
historical behaviour:

- z-score style deviation of the new amount from the user's historical
  mean/spread
- unusually large amount vs history
- results in a risk level plus a human-readable explanation

```text
Normal: ₹500–₹2,000   New: ₹85,000 to a new beneficiary
Result: HIGH ANOMALY RISK  (reason explained)
```

## 5. Validation

- `FraudIntelligenceServiceTest` (4 tests): empty corpus → `NO_DATA`; scam vs
  benign estimates; top signals; metadata.
- `AnomalyServiceTest` (3 tests): normal vs abnormal; meaningful explanations.
- `ScamAnalysisServiceTest` (15 tests): all five blueprint demo cases
  (safe, KYC, OTP, lottery, impersonation) plus the SIH KYC signature demo
  → CRITICAL with 5+ meaningful indicators.
- The learned model is also exposed through the authenticated API
  (`GET/POST /api/fraud/intelligence`), verified by an integration test.

## 6. Future AI extension

The blueprint's Section 16 outlines a Python service (`ai/fraud_model`,
`ai/anomaly_model`, `feature_engineering`, etc.) with endpoints like
`POST /predict-fraud`. This remains the documented extension path; the
in-JVM `FraudIntelligenceService` already implements the equivalent learned
classifier for the current MVP and can be replaced behind the same
`/api/fraud/intelligence` interface.
