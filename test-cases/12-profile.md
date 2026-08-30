# 12 — Financial Profile

Automated coverage: ✅ `FinancialFlowIntegrationTest`

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 12.1 | Get default profile | `GET /api/profile` new user | 200 (may be empty) | |
| 12.2 | Save profile | `PUT /api/profile` full fields | 200 | |
| 12.3 | Retrieve saved | `GET /api/profile` | reflects saved values | |
| 12.4 | Auth required | no token | 401 | |
