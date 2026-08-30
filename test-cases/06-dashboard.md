# 06 — Dashboard

Automated coverage: ✅ `FinancialFlowIntegrationTest`

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 6.1 | Empty dashboard | `GET /api/dashboard` new user | 200, health + fraudSafetyScore, empty lists | |
| 6.2 | After sample data | load sample then GET dashboard | 200, health improved, transactions/alerts present | |
| 6.3 | Auth required | no token | 401 | |
