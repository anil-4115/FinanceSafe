# 02 — Scam Scanner

Automated coverage: ✅ `ScamAnalysisServiceTest` (15)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 2.1 | Safe message → LOW | `POST /api/fraud/analyze` "monthly statement ready...normal banking app" type TEXT | riskLevel `LOW`, few/no indicators | ✅ PASS — `Low` score 4 |
| 2.2 | KYC demo → CRITICAL | scan "account will be blocked...complete KYC...suspicious link...send OTP" | `CRITICAL`, 5+ meaningful indicators, recommendations | ✅ PASS — `Critical` score 99, **7 indicators** |
| 2.3 | OTP scam → HIGH/CRITICAL | scan OTP request + urgency + impersonation | `HIGH` or `CRITICAL` | ✅ PASS (ScamAnalysisServiceTest) |
| 2.4 | Lottery scam → HIGH/CRITICAL | scan prize + processing fee | `HIGH` or `CRITICAL` | ✅ PASS |
| 2.5 | Impersonation → HIGH/CRITICAL | scan fake official + threat + urgency | `HIGH` or `CRITICAL` | ✅ PASS |
| 2.6 | Suspicious URL flag | scan with lookalike/http link | URL indicator present | ✅ PASS — `SUSPECT_TLD` + `WEAK_PROTOCOL` |
| 2.7 | History stored | after scan `GET /api/fraud/history` | contains the scan | ✅ PASS — own scan ids returned |
| 2.8 | History detail | `GET /api/fraud/history/{id}` own id | 200 + indicators | ✅ PASS (FinancialFlow) |
| 2.9 | Cross-user history | request another user's scan id | 404 (no leak) | ✅ PASS — 404 |
| 2.10 | Blank content rejected | `POST /api/fraud/analyze` empty content | 400 | ✅ PASS (validation test) |
