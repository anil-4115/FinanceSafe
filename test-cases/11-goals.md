# 11 — Goals

Automated coverage: ✅ `FinancialFlowIntegrationTest`

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 11.1 | List goals | `GET /api/goals` new user | 200 `[]` | |
| 11.2 | Create goal | `POST /api/goals` | 201 | |
| 11.3 | Goal appears in list | `GET /api/goals` after create | present | |
| 11.4 | Validation | missing name/target | 400 | |
| 11.5 | Auth required | no token | 401 | |
