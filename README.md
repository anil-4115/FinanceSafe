# FinanceSafe — Financial Fraud Assistant

> A fintech + fraud-safety platform built for **Smart India Hackathon (SIH) 2026**.
>
> **An explainable financial safety assistant** combining scam intelligence,
> behavioural anomaly detection, risk scoring, security-health monitoring and
> actionable protection guidance.
>
> **Philosophy:** `DETECT → EXPLAIN → SCORE → PROTECT`

---

## Table of contents

- [What it is](#what-it-is)
- [Features implemented](#features-implemented)
- [How each feature is built](#how-each-feature-is-built)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Roadmap (from scratch)](#roadmap-from-scratch)
- [Run locally](#run-locally)
- [Testing](#testing)
- [Security](#security)
- [Documentation](#documentation)

---

## What it is

FinanceSafe answers four questions for an ordinary user:

```text
1. Is this suspicious?        →  Scam Scanner (rules + AI)
2. Why is it suspicious?      →  Meaningful indicators & reasons
3. How serious is the risk?   →  Risk score + severity (LOW → CRITICAL)
4. What should the user do?   →  Clear recommended actions
```

It protects against the most common consumer frauds — KYC scams, OTP theft,
phishing links, lottery/prize scams, impersonation, and unusual/fraudulent
transactions — in one secure, self-hosted platform.

---

## Features implemented

### 1. Authentication & accounts
- Register, login, logout (JWT bearer tokens)
- BCrypt password hashing, per-user identity from the token

### 2. Scam / fraud scanner (flagship)
- Submit SMS, WhatsApp, email text or a suspicious URL
- Two-stage **hybrid** detection (rule engine + learned model)
- Returns risk score + severity (`LOW`/`MODERATE`/`HIGH`/`CRITICAL`)
- **Explainable output** — list of weighted indicators + recommended actions
- Scan history for the current user
- KYC demo returns `CRITICAL` with 5+ meaningful indicators

### 3. Behavioural anomaly detection
- `POST /api/fraud/transaction-risk` scores a transaction against the user's
  own history
- Detects unusually large amounts, new merchants, deviation from normal
  spending
- Emits a human-readable explanation (never just "suspicious")

### 4. Security / financial health score
- A single 0–100 score summarising overall safety
- 8 components, strengths/weaknesses, and personalised recommendations
- Removes history → `Needs attention`; with sample data → good score

### 5. Transactions
- Manual entry (income / expense) with full CRUD
- **CSV bank-statement import** (up to 1,000 rows, invalid rows reported
  without discarding valid ones)
- Per-transaction risk score and reason

### 6. Alerts
- Generated for high-risk activity, anomalies and safety signals
- List and resolve alerts; **IDOR-safe** (can only resolve your own)

### 7. Community incidents & scam reports
- Users report phone/SMS/WhatsApp/email/UPI/website scams
- Keyword-scored (OTP/PIN, KYC traps, screen sharing, phishing, gift cards)
- Aggregated `GET /api/incidents` feed (newest first, identity-stripped)

### 8. AI / ML intelligence
- A naive-Bayes classifier learned from the app's own corpus
- `GET /api/fraud/intelligence` returns model metadata
- `POST /api/fraud/intelligence/analyze` returns a risk estimate + the
  contributing token signals (explainable AI signal)

### 9. Financial planning
- **Budgets** (one per category, with monthly limit)
- **Financial goals** (target, current, deadline, monthly contribution)
- **Financial profile** (income, expenses, savings, investments, risk
  tolerance)
- **Dashboard** aggregating health, spending, alerts and flagged transactions

### 10. Decision & simulation tools
- `POST /api/decision/analyze` — purchase / loan / investment / payment-request
  decisions with affordability and scam-bait checks
- `POST /api/simulator/investment` — investment growth simulation
- `POST /api/simulator/what-if` — what-if financial scenarios
- `POST /api/investments/recommendation` — personalised allocation

### 11. Education & quiz
- Learning modules on UPI safety, phishing, card fraud, investing, banking
  hygiene
- Per-module quizzes with attempts and a literacy summary

### 12. Products & market
- Financial product catalogue (savings, FD, RD, PPF, mutual funds, gold, ETFs)
  with category filter and **compare**
- Market search + detail (illustrative price/volatility from an in-app
  universe)

### 13. Assistant (chat)
- An in-app assistant chat with persisted conversation history

### 14. Demo data
- `POST /api/demo/load-sample` seeds a realistic profile (transactions,
  budgets, goals, alerts)
- `DELETE /api/demo/clear` resets it for repeatable demos

---

## How each feature is built

| Feature | Service(s) | Notes |
| ------- | ---------- | ----- |
| Auth | `AuthService`, `JwtService` | ISSUE/JWT, `JwtAuthenticationFilter`, BCrypt |
| Scam scanner | `ScamAnalysisService` (rules) + `FraudIntelligenceService` (ML) | ~29 text rules + URL analysis; hybrid risk aggregation via `FraudDetectionService` |
| Anomaly | `AnomalyService` | z-score style deviation vs the user's own history |
| Health score | `HealthScoreService` | composite of 8 weighted components |
| Transactions | `TransactionService`, `FinanceAnalyticsService` | CRUD + CSV importer (`CsvImportService`) |
| Alerts | alert logic in transaction/health flow | persisted `alerts`, resolve scoped by user |
| Incidents | `ScamReportService` | keyword risk scoring on reports |
| AI/ML | `FraudIntelligenceService` | naive-Bayes, Laplace smoothing, log-odds weights |
| Planning | `BudgetService`, `GoalService`, `FinancialProfileService`, `DashboardService` | JPA + scoped by user |
| Decision/sim | `DecisionService`, `SimulatorService`, `InvestmentService` | deterministic calculators |
| Education | `EducationService` | seeded modules + quiz attempts |
| Products/market | `ProductService`, `MarketService` | seeded catalogue + illustrative market universe |
| Assistant | `AssistantService` | rule/guide-based replies, persisted |

All endpoints are **scoped to the authenticated user** via
`CurrentUserService.requireUser()` — the core of IDOR protection.

---

## Architecture

**Modular monolith + single React frontend**, with a hybrid rule + learned
model fraud pipeline running in-JVM (no separate Python service for the MVP).
A Python AI service is the documented future extension point.

```text
                        USER
                          |
                          v
                 +---------------------+
                 | React + Vite (SPA)  |
                 |  + api.js (axios)   |
                 +---------+-----------+
                           |
                       REST / JSON
                  Authorization: Bearer <JWT>
                           |
                           v
              +-----------------------------+
              |        Spring Boot 3.3       |
              |  Security (JWT / CORS)       |
              |  Controllers (REST)          |
              |  Services (business logic)   |
              |  - ScamAnalysisService       |
              |  - FraudIntelligenceService  |
              |  - AnomalyService            |
              |  - HealthScoreService        |
              |  - Dashboard / Tx / Alert... |
              |  Data (Spring Data JPA)      |
              +--------------+--------------+
                             |
                             v
                      PostgreSQL 17
```

### Fraud detection pipeline (hybrid)

```text
Input
  -> Validation
  -> Feature extraction (tokenization / URL parsing)
  -> Rule Engine (deterministic, explainable)
  -> ML/AI Analysis (learned naive-Bayes estimate)
  -> Risk Aggregator (combines signals)
  -> Risk Score + Severity (LOW/MODERATE/HIGH/CRITICAL)
  -> Indicators + Recommendation (explainable output)
```

### Request flow (auth + IDOR safety)

```text
Browser -> login -> JWT
        -> send Authorization: Bearer <JWT>
        -> JwtAuthenticationFilter validates
        -> Controller resolves current user
        -> service scopes every query by that user (IDOR-safe)
```

---

## Tech stack

| Layer | Technology |
| ----- | ---------- |
| Frontend | React, Vite, JavaScript, Tailwind, axios |
| Backend | Java 17, Spring Boot 3.3.3, Spring Security |
| ML/AI | In-JVM naive-Bayes (`FraudIntelligenceService`) |
| Database | PostgreSQL 17, Spring Data JPA / Hibernate |
| Auth | JWT (stateless, Bearer) + BCrypt |
| Build | Maven (backend), npm (frontend) |

---

## Project structure

```text
financial-fraud-assistant/
├── backend/                    # Spring Boot REST API
│   ├── src/main/java/com/financialfraudassistant/
│   │   ├── controller/         # 20 REST controllers
│   │   ├── service/            # business logic + ML
│   │   ├── repository/         # Spring Data JPA
│   │   ├── model/              # 15 JPA entities
│   │   ├── dto/                # request/response records
│   │   ├── config/             # security, JWT filter, seeder
│   │   └── exception/          # global error handling
│   └── src/test/java/          # 75 automated tests
├── frontend/                   # React + Vite SPA
├── docs/                       # architecture, api, db, security, ai-ml, sih, deployment
├── test-cases/                 # per-feature manual/API test sheets + results
├── database/schema/            # SQL schema init (Postgres)
├── ai/                         # (reserved) future Python AI extension
├── docker-compose.yml          # PostgreSQL 17
└── .env.example                # env templates
```

---

## Roadmap (from scratch)

The build followed the master blueprint in
`docs/FinanceSafe_Project_Blueprint.md`. Every phase is **done**:

| # | Phase | Deliverable | Status |
| -- | ----- | ----------- | ------ |
| 1 | **Foundation** | Project structure, frontend/backend setup, PostgreSQL, env, health check | ✅ |
| 2 | **Core application** | Auth + JWT, main APIs, frontend pages, DB integration | ✅ |
| 3 | **Frontend/API verification** | All pages ↔ APIs verified, UI/API mismatches fixed, local comms confirmed | ✅ |
| 4 | **Fraud intelligence** | Rule engine hardened (KYC/OTP/urgency/impersonation/lottery/URL), weights tuned, all demo cases verified | ✅ |
| 5 | **AI/anomaly validation** | Anomaly verified, AI responses validated, AI service connected (`/api/fraud/intelligence`) | ✅ |
| 6 | **Backend + security testing** | Unit/integration/auth/JWT/scanner/anomaly/health/alert/IDOR/validation tests | ✅ |
| 7 | **SIH documentation** | Architecture, API, database, AI/ML, security, innovation, demo story | ✅ |
| 8 | **Deployment** | Env vars, configurable CORS, PostgreSQL, smoke tests, deployment doc | ✅ |
| 9 | **Final demo** | End-to-end register→login→scan→anomaly→health→alert→intelligence→IDOR verified | ✅ |

### Notable milestones added during the roadmap

- **AI validation (Phase 5):** wired `FraudIntelligenceService` into the API
  (`FraudIntelligenceController` + `IntelligenceAnalyzeRequest`) so the ML
  stage is demonstrable.
- **Deployment (Phase 8):** made CORS configurable via `CORS_ALLOWED_ORIGIN`
  (was hard-coded to localhost).
- **Pre-deploy QA:** added `test-cases/` with per-feature sheets and a
  `FeatureCoverageIntegrationTest` (10 tests) that closed the automated-coverage
  gaps on education, products, market, assistant, decision/simulator,
  incidents, alert-resolve and demo data.

---

## Run locally

Prerequisites: Java 17, Maven, Node.js 20+, Docker Desktop.

```powershell
# 1. Start PostgreSQL
docker compose up -d

# 2. Start the API (from backend/) — Java 21 JDK located at D:\Softwares\JDKKK
cmd /c "set JAVA_HOME=D:\Softwares\JDKKK& set PATH=D:\Softwares\JDKKK\bin;%PATH%& mvn spring-boot:run"
# → http://localhost:8080  (health: GET /api/health)

# 3. Start the web app (from frontend/)
npm install
npm run dev
# → http://localhost:5173
```

Environment templates: `frontend/.env.example`, `backend/.env.example`.
Never commit real credentials or production JWT secrets.

---

## Testing

**75 automated tests, all passing.**

```powershell
cd backend
cmd /c "set JAVA_HOME=D:\Softwares\JDKKK& set PATH=D:\Softwares\JDKKK\bin;%PATH%& mvn test"
```

Coverage includes:

- Auth & JWT (register/login/expired/tampered)
- Scam scanner quality (5 demo cases + SIH KYC → CRITICAL 99, 7 indicators)
- Anomaly detection, health score, alert & financial flow
- Security & IDOR (401/403, cross-user 404, secret stripping)
- AI/ML responses (metadata, estimates, explainable signals)
- Every feature endpoint (education, products, market, assistant, decision,
  simulator, incidents, alerts, demo data)

Manual/API test sheets with recorded results live in
**[`test-cases/`](test-cases/README.md)**.

---

## Security

- Stateless **JWT** auth, **BCrypt** password hashing
- **IDOR protection** — all user-scoped queries filtered by the authenticated
  user (cross-user access → 404)
- Input validation + consistent, non-leaking `ApiError` responses
- Restricted **CORS** (configurable via `CORS_ALLOWED_ORIGIN`)
- Secrets from environment variables (never hard-coded for production)
- **Fixed during QA:** loan decision `ArithmeticException` (500) on fractional
  interest rates — now handled with an explicit rounding mode

---

## Documentation

| Doc | Location |
| --- | -------- |
| Architecture | `docs/architecture/architecture.md` |
| API reference | `docs/api/api-documentation.md` |
| Database design | `docs/database/database-design.md` |
| Security | `docs/security/security.md` |
| AI/ML | `docs/ai-ml/ai-ml.md` |
| Deployment | `docs/deployment/deployment.md` |
| SIH problem/innovation/demo/judging | `docs/sih/` |
| Master blueprint | `docs/FinanceSafe_Project_Blueprint.md` |
| Test cases & results | `test-cases/README.md` |

---

> **FinanceSafe does not merely detect fraud — it explains risk and helps
> users make safer financial decisions.**
