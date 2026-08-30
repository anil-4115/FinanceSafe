# FinanceSafe — Manual / API Test Cases

> Pre-deployment verification suite. Each file describes **scenarios → steps →
> expected result → actual result** for a feature. The "Actual" column is
> filled from live execution.
>
> **How results are marked:** `PASS` / `FAIL` / `N/A` (not applicable).
> Automated coverage is noted per sheet and verified by the Maven suite
> (`mvn test`, green = all pass).

---

## Test environment

| Item | Value |
| ---- | ----- |
| Backend | Spring Boot, `http://localhost:8080` |
| Frontend | React + Vite, `http://localhost:5173` |
| Database | PostgreSQL 17 `financial_fraud_assistant` |
| JWT | returned by register/login, sent as `Authorization: Bearer <token>` |

---

## Feature matrix (overview)

| # | Feature | Sheet | Automated? | Live result |
| -- | ------- | ----- | ---------- | ----------- |
| 1 | Authentication (register/login) | `01-auth.md` | ✅ AuthIntegrationTest | ✅ PASS |
| 2 | Scam Scanner (analyze/history) | `02-scam-scanner.md` | ✅ ScamAnalysisServiceTest | ✅ PASS |
| 3 | Transaction CRUD + CSV import | `03-transactions.md` | ✅ FinancialFlowIntegrationTest | ✅ PASS |
| 4 | Anomaly / transaction-risk | `04-anomaly.md` | ✅ AnomalyServiceTest | ✅ PASS |
| 5 | Health score | `05-health-score.md` | ✅ HealthScoreServiceTest | ✅ PASS |
| 6 | Dashboard | `06-dashboard.md` | ✅ FinancialFlowIntegrationTest | ✅ PASS |
| 7 | Alerts (list/resolve) | `07-alerts.md` | ✅ FeatureCoverageIntegrationTest | ✅ PASS |
| 8 | Fraud reports / incidents | `08-incidents.md` | ✅ FeatureCoverageIntegrationTest | ✅ PASS |
| 9 | AI/ML intelligence | `09-ai-ml.md` | ✅ FraudIntelligenceServiceTest | ✅ PASS |
| 10 | Budgets | `10-budgets.md` | ✅ FinancialFlowIntegrationTest | ✅ PASS |
| 11 | Goals | `11-goals.md` | ✅ FinancialFlowIntegrationTest | ✅ PASS |
| 12 | Financial profile | `12-profile.md` | ✅ FinancialFlowIntegrationTest | ✅ PASS |
| 13 | Education + quiz | `13-education.md` | ✅ FeatureCoverageIntegrationTest | ✅ PASS |
| 14 | Products & compare | `14-products.md` | ✅ FeatureCoverageIntegrationTest | ✅ PASS |
| 15 | Market | `15-market.md` | ✅ FeatureCoverageIntegrationTest | ✅ PASS |
| 16 | Assistant (chat) | `16-assistant.md` | ✅ FeatureCoverageIntegrationTest | ✅ PASS |
| 17 | Decision & simulator | `17-decision-simulator.md` | ✅ FeatureCoverageIntegrationTest | ✅ PASS (bug fixed) |
| 18 | Demo data (load/clear) | `18-demo-data.md` | ✅ FeatureCoverageIntegrationTest | ✅ PASS |
| 19 | Security & IDOR | `19-security.md` | ✅ SecurityIntegrationTest | ✅ PASS |
| 20 | Deployment smoke | `20-deployment-smoke.md` | ✅ health + tests | ✅ PASS |

Legend: ✅ covered · ❌ missing automated coverage · ⚠️ partial

---

## Sheets

- [01-auth.md](01-auth.md)
- [02-scam-scanner.md](02-scam-scanner.md)
- [03-transactions.md](03-transactions.md)
- [04-anomaly.md](04-anomaly.md)
- [05-health-score.md](05-health-score.md)
- [06-dashboard.md](06-dashboard.md)
- [07-alerts.md](07-alerts.md)
- [08-incidents.md](08-incidents.md)
- [09-ai-ml.md](09-ai-ml.md)
- [10-budgets.md](10-budgets.md)
- [11-goals.md](11-goals.md)
- [12-profile.md](12-profile.md)
- [13-education.md](13-education.md)
- [14-products.md](14-products.md)
- [15-market.md](15-market.md)
- [16-assistant.md](16-assistant.md)
- [17-decision-simulator.md](17-decision-simulator.md)
- [18-demo-data.md](18-demo-data.md)
- [19-security.md](19-security.md)
- [20-deployment-smoke.md](20-deployment-smoke.md)

---

## Verification results (latest run)

- **Automated suite:** `mvn test` in `backend/` → **75 tests, 0 failures, 0 errors**.
- **New automated coverage added:** `FeatureCoverageIntegrationTest` (10 tests) for
  features that had no backend test: education/quiz, products/compare, market,
  assistant, decision/simulator/investments, fraud reports/incidents, alert
  resolve + IDOR, demo data.
- **Live end-to-end smoke** against the running app also passed for
  register/login/dashboard/scam/anomaly/health/alerts/AI/intelligence/IDOR.

### Bug found & fixed during testing

- `POST /api/decision/analyze` with a **fractional interest rate** (e.g. `10.5`)
  crashed with `ArithmeticException: Rounding necessary` → HTTP 500.
  - Root cause: `interest.setScale(0)` without a rounding mode at
    `DecisionService.java:108`.
  - Fix: `interest.setScale(0, RoundingMode.HALF_UP)`.
  - The test `decisionAnalyze_requiresAuthAndReturnsGuidance` guards this case.

---

## Where to keep an eye (watch-list before deploy)

1. **API auth on all `/api/**`** — everything except `GET /api/health` and
   `/api/auth/**` requires a JWT. Some endpoints you might assume are public
   (`/api/investments/recommendation`, `/api/simulator/investment`) are **in
   fact protected** (return 401 without a token). If the frontend calls these
   without auth it will get 401 — make sure the SPA sends the JWT or these are
   intentionally kept protected.
2. **`DB_PASSWORD` / `JWT_SECRET` defaults** — dev defaults (`postgres`,
   `development-only-secret-...`) must be overridden in production. The
   `JWT_SECRET` default is especially important; never deploy with it.
3. **`CORS_ALLOWED_ORIGIN`** — defaults to `http://localhost:5173`. Set it to
   the real production frontend origin or browser calls will be blocked.
4. **Anomaly engine needs history** — a fresh account reports "not enough
   history" (correct). For the demo, load sample data first
   (`POST /api/demo/load-sample`).
5. **`spring.jpa.hibernate.ddl-auto=update`** — convenient but implicit; for a
   hardened release prefer explicit migrations (`database/schema`).
6. **Decision service rounding** — the root cause was a `setScale` without a
   rounding mode. If more decimal-heavy calculations are added, prefer explicit
   scales with a rounding mode.

