# Baseline Checkpoint — Phase 1 Freeze

Captured on 2026-08-30 against the working, verified baseline.

## Version control

- Git repository initialised at the project root.
- Tag: `v0.1.0-baseline`
- Commit: `9392933`
- Backup zip: `backups/financial-fraud-assistant-v0.1.0-baseline.zip` (clean `git archive` snapshot, excludes `node_modules`, `target`, `dist`, `.env`).

## Runtime state at freeze

- Backend: Spring Boot on `http://localhost:8080` — `GET /api/health` returns `UP`.
- Database: PostgreSQL (native, port 5432) — database `financial_fraud_assistant`.
- Frontend: Vite dev server on `http://localhost:5173`.

## API surface (all under `/api`)

### Public

| Method | Endpoint | Purpose |
|---|---|---|
| GET  | `/health` | health check |
| POST | `/auth/register` | create account |
| POST | `/auth/login` | authenticate, returns JWT |

### Authenticated

| Method | Endpoint | Purpose |
|---|---|---|
| GET    | `/profile` | financial profile |
| GET    | `/dashboard` | dashboard summary |
| GET    | `/transactions` | transaction list |
| PUT    | `/transactions/{id}` | update transaction |
| DELETE | `/transactions/{id}` | delete transaction |
| POST   | `/transactions/import` | CSV import |
| GET    | `/budgets` | budget list |
| POST   | `/budgets` | create budget |
| GET    | `/goals` | goals list |
| POST   | `/goals` | create goal |
| GET    | `/health-score` | financial health score |
| GET    | `/products` | financial products |
| GET    | `/products/compare` | product comparison |
| GET    | `/products/{id}` | product detail |
| GET    | `/investments` | investments |
| POST   | `/investments/recommendation` | investment recommendation |
| GET    | `/market/search` | stock search |
| GET    | `/market/{symbol}` | stock detail |
| POST   | `/simulator/investment` | investment simulation |
| POST   | `/simulator/what-if` | what-if simulation |
| POST   | `/fraud/analyze` | scam/message/URL analysis |
| POST   | `/fraud/transaction-risk` | transaction risk assessment |
| GET    | `/fraud/history` | fraud analysis history |
| GET    | `/fraud/history/{id}` | single analysis |
| POST   | `/fraud/reports` | scam report |
| GET    | `/alerts` | alert list |
| PATCH  | `/alerts/{alertId}/resolve` | resolve alert |
| POST   | `/decision/analyze` | decision safety engine |
| GET    | `/incidents` | incident reports list |
| GET    | `/education` | education modules |
| GET    | `/education/literacy` | literacy score |
| GET    | `/education/{id}` | module detail |
| GET    | `/education/{id}/quiz` | module quiz |
| POST   | `/education/{id}/attempt` | quiz attempt |
| POST   | `/assistant/chat` | AI assistant chat |
| GET    | `/assistant/history` | chat history |
| POST   | `/demo/load-sample` | load demo data |
| DELETE | `/demo/clear` | clear demo data |

## Database schema

Stored at `database/schema/01_init.sql`. Hibernate additionally manages the
following tables via `ddl-auto=update`: `financial_products`, `fraud_analysis`,
`fraud_indicators`, `education_modules`, `quiz_questions`, `education_attempts`,
`chat_conversations`, `chat_messages`, `scam_reports`, `alerts`, plus audit/user
extensions.

Ownership rule: every financial record table carries `user_id` and every service
lookup is scoped to the authenticated user's id.