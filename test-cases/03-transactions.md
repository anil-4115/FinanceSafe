# 03 — Transactions (CRUD + CSV import)

Automated coverage: ✅ `FinancialFlowIntegrationTest`

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 3.1 | List empty | `GET /api/transactions` new user | 200 `[]` | |
| 3.2 | Create income | `POST /api/transactions` income | 201 | |
| 3.3 | Create expense | `POST /api/transactions` expense | 201 | |
| 3.4 | List after create | `GET /api/transactions` | 200, count matches | |
| 3.5 | Update transaction | `PUT /api/transactions/{id}` | 200, changed | |
| 3.6 | Delete transaction | `DELETE /api/transactions/{id}` | 204, gone from list | |
| 3.7 | Negative amount rejected | create amount `-50` | 400 | |
| 3.8 | CSV import valid | `POST /api/transactions/import` with valid CSV | 200 + count imported | |
| 3.9 | CSV import invalid rows | CSV with some bad rows | 200, bad rows reported, valid kept | |
| 3.10 | Cross-user transaction access | user B GET/PUT user A's id | 404 | |
