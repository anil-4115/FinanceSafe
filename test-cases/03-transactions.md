# 03 — Transactions (CRUD + CSV import)

Automated coverage: ✅ `FinancialFlowIntegrationTest` (incl. merchant-optional CSV import tests)

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
| 3.11 | CSV without `merchant` column | CSV with only `date`, `amount`, `type`, `category` | 200, rows imported as `Unknown` merchant | |
| 3.12 | CSV merchant synonym | CSV uses `narration` (or `description`/`payee`) as the merchant column | 200, merchant taken from that column | |
| 3.13 | CSV amount synonyms | `credit`/`debit` (or `deposit`/`withdrawal`) columns instead of `amount` | 200, credit→income, debit→expense | |
| 3.14 | CSV no amount-like column | CSV with neither `amount` nor credit/debit | 400 with clear error naming accepted headers | |
