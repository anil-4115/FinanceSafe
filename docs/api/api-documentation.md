# FinanceSafe — REST API Documentation

> Base URL: `http://localhost:8080` (local). All JSON.
>
> **Authentication:** every endpoint except `GET /api/health` and
> `/api/auth/**` requires `Authorization: Bearer <JWT>`. Obtain a token from
> `POST /api/auth/login`.

---

## 1. Standard error responses

Errors use `ApiError` from the global handler:

```json
{
  "success": false,
  "message": "Human readable reason",
  "errorCode": "VALIDATION_ERROR",
  "status": 400,
  "timestamp": "2026-08-30T12:00:00.000Z",
  "details": {}
}
```

| Status | Meaning |
| ------ | ------- |
| 400 | Validation / bad request (`VALIDATION_ERROR`) |
| 401 | Missing or invalid JWT (`UNAUTHORIZED`) |
| 403 | Authenticated but not allowed (`FORBIDDEN`) |
| 404 | Resource not found for the current user |
| 405 | Method not allowed |
| 413 | Payload too large |
| 500 | Unexpected server error |

---

## 2. Authentication

### POST `/api/auth/register`

Create a new account. Returns a signed JWT.

**Request**

```json
{ "email": "demo@example.com", "password": "SecurePass123", "fullName": "Demo User" }
```

**Response `201 Created`**

```json
{ "token": "<JWT>", "user": { "id": 1, "email": "demo@example.com", "fullName": "Demo User" } }
```

### POST `/api/auth/login`

**Request**

```json
{ "email": "demo@example.com", "password": "SecurePass123" }
```

**Response** — same shape as register.

---

## 3. Public / system endpoints

### GET `/api/health`

```json
{ "status": "UP", "service": "financial-fraud-assistant", "timestamp": "..." }
```

### GET `/api/fraud/intelligence`

Model metadata for the ML analysis stage.

```json
{
  "stages": ["User Input", "Input Classification", "Indicator / Feature Extraction",
             "Rule Engine", "ML/AI Analysis", "Risk Score", "Risk Level",
             "Explanation", "Recommended Action"],
  "ruleWeight": 0.7,
  "aiWeight": 0.3,
  "baseRate": 0.2,
  "scamDocs": 12,
  "benignDocs": 8,
  "vocabSize": 340,
  "builtAt": "2026-08-30T12:00:00",
  "topSignals": ["kyc (+2.4)", "otp (+2.1)", "blocked (+1.8)"]
}
```

### POST `/api/fraud/intelligence/analyze`

**Request**

```json
{ "content": "Your bank account will be blocked, share your OTP to verify." }
```

**Response**

```json
{
  "estimate": 88,
  "signals": ["otp (+2.1)", "blocked (+1.8)", "verify (+1.2)"]
}
```

> `estimate` is `0..99`, or `-1` (`NO_DATA`) when the model has no training
> corpus yet. `signals` are the positive contributing tokens, strongest first.

### POST `/api/investments/recommendation`

Public recommendation endpoint (no auth).

### POST `/api/simulator/investment`

Public investment simulation endpoint (no auth).

---

## 4. Fraud scanner

### POST `/api/fraud/analyze`

Scan text or a URL for scams. Core explainable detection endpoint.

**Request**

```json
{ "content": "Your bank account will be blocked today.", "type": "TEXT" }
```

**Response**

```json
{
  "riskScore": 92,
  "riskLabel": "CRITICAL",
  "scamType": "OTP",
  "category": "account_blocking",
  "confidence": "HIGH",
  "summary": "Urgent account-blocking threat detected...",
  "aiEstimate": 88,
  "indicators": [
    { "kind": "URGENCY", "label": "Urgent account-blocking threat", "weight": 25 },
    { "kind": "KYC", "label": "KYC request detected", "weight": 25 }
  ],
  "recommendations": ["Do not share the OTP.", "Verify via the official channel."]
}
```

### POST `/api/fraud/transaction-risk`

Assess a transaction for anomaly risk.

### GET `/api/fraud/history`

List the current user's scan history (newest first).

### GET `/api/fraud/history/{id}`

Get one scan. Returns `404` if it does not belong to the current user (IDOR-safe).

### POST `/api/fraud/reports`

Submit a community scam report. Returns `{ "riskScore": <int> }`, `201 Created`.

**Request**

```json
{ "channel": "SMS", "description": "Fake bank security alert", "amountAtRisk": 0 }
```

---

## 5. Transactions

| Method | Path | Notes |
| ------ | ---- | ----- |
| GET | `/api/transactions` | List current user's transactions |
| POST | `/api/transactions` | Create (returns `201`) |
| PUT | `/api/transactions/{id}` | Update |
| DELETE | `/api/transactions/{id}` | Delete (`204`) |
| POST | `/api/transactions/import` | CSV import (`multipart`, param `file`) |

---

## 6. Alerts

| Method | Path | Notes |
| ------ | ---- | ----- |
| GET | `/api/alerts` | Current user's alerts |
| PATCH | `/api/alerts/{alertId}/resolve` | Resolve an alert (`404` if not owned) |

---

## 7. Dashboard & health

| Method | Path | Notes |
| ------ | ---- | ----- |
| GET | `/api/dashboard` | Aggregated overview: health score, alerts, recent activity |
| GET | `/api/health-score` | Security/financial health score with reasons & recommendations |
| GET | `/api/incidents` | Community incidents feed (max 50, newest first) |

---

## 8. Profile & goals

| Method | Path | Notes |
| ------ | ---- | ----- |
| GET / PUT | `/api/profile` | Financial profile |
| GET / POST | `/api/goals` | Financial goals |

---

## 9. Budgets

| Method | Path | Notes |
| ------ | ---- | ----- |
| GET | `/api/budgets` | List budgets |
| POST | `/api/budgets` | Create budget (`201`). One per (user, category). |

---

## 10. Assistant (chat)

| Method | Path | Notes |
| ------ | ---- | ----- |
| POST | `/api/assistant/chat` | Ask the assistant |
| GET | `/api/assistant/history` | Chat history |

---

## 11. Decisions & simulation

| Method | Path | Notes |
| ------ | ---- | ----- |
| POST | `/api/decision/analyze` | Decision-safety analysis |
| POST | `/api/simulator/what-if` | What-if financial simulation |

---

## 12. Education

| Method | Path | Notes |
| ------ | ---- | ----- |
| GET | `/api/education` | Learning modules |
| GET | `/api/education/literacy` | Literacy summary |
| GET | `/api/education/{id}` | Module detail |
| GET | `/api/education/{id}/quiz` | Quiz questions for module |
| POST | `/api/education/{id}/attempt` | Submit a quiz attempt |

---

## 13. Products, markets & comparison

| Method | Path | Notes |
| ------ | ---- | ----- |
| GET | `/api/products?category=` | Product catalogue |
| GET | `/api/products/{id}` | Product detail |
| GET | `/api/products/compare?ids=1,2` | Compare products |
| GET | `/api/market/search?q=` | Market search |
| GET | `/api/market/{symbol}` | Market detail |

---

## 14. Demo data

| Method | Path | Notes |
| ------ | ---- | ----- |
| POST | `/api/demo/load-sample` | Load sample data for DJ/demo |
| DELETE | `/api/demo/clear` | Remove sample data |

---

## 15. Deployment environment (backend)

| Variable | Default (dev) | Purpose |
| -------- | -------------- | ------- |
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/financial_fraud_assistant` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | `postgres` | DB password |
| `JWT_SECRET` | development value | Signing secret — **must** be overridden in production |

Frontend: `VITE_API_BASE_URL` (see `frontend/.env.example`).
