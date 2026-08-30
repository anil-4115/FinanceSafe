# 03 — Transactions (CRUD + CSV import)

Automated coverage: ✅ `FinancialFlowIntegrationTest` (incl. merchant-optional, amount-synonym, date-format, needs-attention, currency-parsing tests)

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
| 3.9 | CSV import invalid rows | CSV with some bad rows | 200, bad rows under `needsAttention`, valid rows kept | |
| 3.10 | Cross-user transaction access | user B GET/PUT user A's id | 404 | |
| 3.11 | CSV without `merchant` column | CSV with only `date`, `amount`, `type`, `category` | 200, rows imported as `Unknown` merchant | |
| 3.12 | CSV merchant synonym | CSV uses `narration` (or `particulars`/`description`/`payee`) as the merchant column | 200, merchant taken from that column | |
| 3.13 | CSV amount synonyms | `Credit`/`Debit` (or `Deposit`/`Withdrawal`) split columns instead of `amount` | 200, credit→income, debit→expense | |
| 3.14 | CSV no amount-like column | CSV with neither `amount` nor credit/debit | 400 with clear error naming detected columns | |
| 3.15 | Withdrawal/Deposit Amt headers | `Withdrawal Amt.` / `Deposit Amount` columns | 200, withdrawal→expense, deposit→income | |
| 3.16 | Date auto-detection | `Transaction Date`/`Txn Date`/`Value Date` col; values in dd/MM/yyyy, dd-MMM-yyyy, dd.MM.yyyy | 200, all dates parsed | |
| 3.17 | Unknown columns ignored | Statement with `Ref No.`, `Branch` etc. | 200, only relevant columns used | |
| 3.18 | Currency & thousands | `Rs 1,500.00`, `(12000)` parenthesized negative | 200, correct amount + sign | |
| 3.19 | Type word overrides sign | `type` = `credit`/`debit` with positive amount | 200, type honored | |

## Spending dashboard (frontend)

Manual coverage: Spending page (`frontend/src/pages/Spending.jsx`)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 3.20 | Summary cards | Dashboard cards | Balance / Income / Spent / Suspicious computed from transactions | |
| 3.21 | Spending trend filters | click 7D/30D/3M/1Y | Chart updates; shows real transaction income/expense | |
| 3.22 | Category chart | Spending by category panel | Donut of expense categories from real data | |
| 3.23 | Safety status | Transaction safety panel | 🟢 Normal / 🟡 Review / 🔴 Suspicious derived from `riskLevel` | |
| 3.24 | Suspicious alert | suspicious transaction present | Shows reason from `riskReason` (or stored risk score) + Review link | |
| 3.25 | Recent list | main page | Last ~6 transactions + View All | |
| 3.26 | View All modal | open modal | Search, type/category/safety filters, sort by date/amount, pagination | |
| 3.27 | Empty state | no transactions | Friendly messages, no breakage | |
| 3.28 | Responsive | mobile/tablet widths | 4→2→1 column grids, modal usable | |
