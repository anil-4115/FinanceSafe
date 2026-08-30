# FinanceSafe — Demo Story (SIH)

A single, realistic end-to-end story that runs the whole product in one flow.

---

## Step 1 — Login

User opens FinanceSafe and logs in (or the sample data is pre-loaded via
`POST /api/demo/load-sample`).

## Step 2 — Dashboard

The dashboard shows the **Security Health Score**, open alerts and recent
activity — an instant overview of the account's safety.

## Step 3 — A scam message arrives

The user pastes the KYC signature demo:

```text
Your bank account will be blocked today.
Complete KYC immediately:
http://suspicious-link.example
Send your OTP to verify.
```

## Step 4 — Detection

FinanceSafe returns:

```text
CRITICAL
Risk Score: 90+
```

## Step 5 — Explanation (the "why")

```text
Indicators:
✓ KYC request
✓ OTP request
✓ Urgency (act now / account will be blocked)
✓ Account-blocking threat
✓ Suspicious URL

Recommended actions:
- Do not share the OTP or credentials.
- Do not click the link.
- Verify via the official banking channel.
```

This is the **DETECT → EXPLAIN → SCORE → PROTECT** moment.

## Step 6 — Transaction anomaly

Open a transaction far outside the user's normal behaviour
(e.g. ₹85,000 to a new beneficiary when normal spending is ₹500–₹2,000).

```text
HIGH ANOMALY RISK
Reason: amount deviates significantly from historical pattern / new beneficiary
```

## Step 7 — Alert

A **security alert** is generated and appears on the dashboard, tying the
scam scan and the anomaly into the health score.

## Step 8 — Security

Demonstrate IDOR protection: attempting to change a resource ID (a
transaction, fraud scan, or alert belonging to another user) returns **404**
and never exposes the other user's data.

## Step 9 — Closing statement

> **FinanceSafe does not merely detect fraud — it explains risk and helps
> users make safer financial decisions.**

---

## Scripted talking points

1. Why it matters (KYC/OTP scams hurt real people).
2. What makes it different (explainable + hybrid + secure).
3. Walk the story end to end.
4. Close on safety, trust and empowerment.
