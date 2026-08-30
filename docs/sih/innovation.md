# FinanceSafe — Innovation & Differentiation (SIH)

FinanceSafe is **not** just "an AI fraud detector". It is positioned as:

> **An explainable financial safety assistant combining scam intelligence,
> behavioural anomaly detection, risk scoring, security-health monitoring and
> actionable protection guidance.**

---

## Key differentiators

### 1. Explainable detection
We move beyond "Fraud probability: 92%". Every result shows the **why** —
specific indicators with weights — and **what to do next**. This builds trust
and is far more useful to a real user.

```text
CRITICAL — 92/100
Why?
 1. KYC request
 2. OTP request
 3. Urgency
 4. Suspicious URL
 5. Impersonation
What to do?
 - Do not share the OTP.
 - Do not click the link.
 - Verify using the official channel.
```

### 2. Hybrid intelligence (rules + learned model)
- **Rules** give fast, deterministic, explainable detection of known patterns.
- A **naive-Bayes learned model** (trained on the app's own scans and
  community reports) captures subtle signals and generalises beyond fixed
  rules — implemented natively in Java with no extra runtime dependency.

### 3. Multi-signal risk scoring
A score reflects **meaningful, non-duplicated evidence** rather than an
artificially inflated count. Severity thresholds map cleanly to
LOW/MODERATE/HIGH/CRITICAL.

### 4. Behavioural anomaly detection
Detects transactions that deviate from a user's own historical behaviour
(e.g. a large amount to a new beneficiary) and explains the deviation — not
just "suspicious".

### 5. Security / financial health score
A single score summarises overall safety with clear reasons, changes over
time and recommendations — giving the user an at-a-glance picture.

### 6. Security-by-design
JWT authentication, per-user authorization, **IDOR protection**, input
validation and safe error handling are built in and verified by tests —
essential for anything handling financial data.

### 7. Actionable recommendations
Every detection leads to concrete "what to do now" guidance, closing the
loop from detection to protection.

---

## Demo assets built in

- Five fixed scanner demo cases (safe / KYC / OTP / lottery / impersonation)
  verified by tests, including a **KYC signature demo → CRITICAL with 5+
  meaningful indicators**.
- Sample-data loader for a smooth end-to-end demonstration.
- A single end-to-end story (Login → Dashboard → Scam → Explain → Anomaly →
  Alert → Security) suitable for the SIH pitch.
