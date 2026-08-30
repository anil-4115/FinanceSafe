# 09 — AI / ML Intelligence

Automated coverage: ✅ `FraudIntelligenceServiceTest` (4) + integration

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 9.1 | Metadata | `GET /api/fraud/intelligence` (with token) | ruleWeight 0.7, aiWeight 0.3, corpus counts | ✅ PASS — 0.7 / 0.3, trained vocab |
| 9.2 | Analyze scam text | `POST /api/fraud/intelligence/analyze` "blocked/OTP" | estimate 0..99 + positive signals | ✅ PASS — estimate 94 |
| 9.3 | Explainable signals | inspect `signals` | have `+weight` tokens | ✅ PASS — "blocked (+1.5)", "verify (+1.4)" |
| 9.4 | Auth required | endpoint without token | 401 | ✅ PASS |
