# 10 — Budgets

Automated coverage: ✅ `FinancialFlowIntegrationTest`

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 10.1 | List budgets | `GET /api/budgets` new user | 200 `[]` | |
| 10.2 | Create budget | `POST /api/budgets` | 201 | |
| 10.3 | Duplicate category | create same (user,category) twice | 2nd → 4xx conflict | |
| 10.4 | Auth required | no token | 401 | |
