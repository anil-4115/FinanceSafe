# 18 — Demo Data (load / clear)

Automated coverage: ✅ `FeatureCoverageIntegrationTest` (demoData_loadAndClear)

Also verified live against the running app: `POST /api/demo/load-sample` loads
21 transactions, 4 budgets, 2 goals + 1 alert (health 43 → 84), and
`DELETE /api/demo/clear` clears them.

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | ----- | -------- |
| 18.1 | Load sample data | `POST /api/demo/load-sample` | 200 + counts (transactions/budgets/goals/alerts) | |
| 18.2 | Idempotent load | call load-sample twice | 2nd → `alreadyLoaded` true, no duplicate blow-up | |
| 18.3 | Clear demo data | `DELETE /api/demo/clear` | 200 | |
| 18.4 | Auth required | no token | 401 | |
