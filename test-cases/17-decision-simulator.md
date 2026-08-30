# 17 — Decision & Simulator

Automated coverage: ✅ `FeatureCoverageIntegrationTest` (decision/analyze, simulator/investment, simulator/what-if, investments/recommendation)

> ⚠️ Important finding: I fixed a **real bug** here — `POST /api/decision/analyze`
> with a fractional interest rate (e.g. `10.5`) crashed with
> `ArithmeticException: Rounding necessary` (500). Fixed by adding
> `RoundingMode.HALF_UP` at `DecisionService.java:108`.
>
> Note: simulator/investment and investments/recommendation are technically
> **not public** — the security filter requires a JWT on all `/api/**` (test
> verifies 401 without a token). They still work fine with a token.

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 17.1 | Decision analyze | `POST /api/decision/analyze` + token | 200 + guidance | ✅ PASS (after bug fix) |
| 17.2 | Investment simulator | `POST /api/simulator/investment` + token | 200 | ✅ PASS |
| 17.3 | What-if (auth) | `POST /api/simulator/what-if` + token | 200 | ✅ PASS |
| 17.4 | Investment recommendation | `POST /api/investments/recommendation` + token | 200 | ✅ PASS |
| 17.5 | Validation | decision/analyze missing field | 400 | ✅ PASS |
| 17.6 | What-if auth required | what-if without token | 401 | ✅ PASS |
