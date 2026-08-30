# FinanceSafe — System Architecture

> _An explainable financial safety assistant combining scam intelligence,
> behavioural anomaly detection, risk scoring, security-health monitoring and
> actionable protection guidance._

---

## 1. High-level view

FinanceSafe is built as a **modular monolith** backend plus a **single-page
React frontend**, backed by PostgreSQL. A hybrid (rule-engine + learned-model)
fraud pipeline lives inside the Spring Boot backend, so no separate Python
service is required for the current MVP.

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
              |                             |
              | Security (JWT filter, CORS) |
              | Controllers (REST)          |
              | Services (business logic)   |
              |   - ScamAnalysisService     |
              |   - FraudDetectionService   |
              |   - AnomalyService          |
              |   - FraudIntelligenceService|
              |   - HealthScoreService      |
              |   - DashboardService        |
              | Data (Spring Data JPA)      |
              +--------------+--------------+
                             |
                             v
                      PostgreSQL 17
                (financial_fraud_assistant)
```

## 2. Technology stack

| Layer        | Technology                                   |
| ------------ | -------------------------------------------- |
| Frontend     | React, Vite, JavaScript, HTML/CSS, axios     |
| Backend      | Java 17, Spring Boot 3.3.3, Spring Security  |
| ML/AI        | In-JVM naive-Bayes model (`FraudIntelligenceService`) |
| Database     | PostgreSQL 17, Spring Data JPA / Hibernate   |
| Build        | Maven (backend), npm (frontend)              |
| Auth         | JWT (stateless, Bearer token)                |

> **AI/ML note:** the blueprint's Section 16 describes a separate Python
> service. For this MVP the ML stage is implemented natively in Java as a
> naive-Bayes classifier learned from the application's own labelled scans and
> community scam reports. This keeps the deployment a single runtime while
> still delivering a genuine, explainable learned model. A Python service is
> the documented future extension point.

## 3. Request flow (auth + authorization)

All `/api/**` endpoints except `GET /api/health` and `/api/auth/**` require a
valid JWT.

```text
Browser
  -> POST /api/auth/login  -> AuthService -> JWT
  -> store token           -> send as: Authorization: Bearer <JWT>
  -> JwtAuthenticationFilter validates token
  -> SecurityContext populated
  -> Controller resolves current User via CurrentUserService.requireUser()
  -> service scopes every query by that user (IDOR-safe)
```

## 4. Fraud detection pipeline

```text
Input
  -> Validation
  -> Feature extraction (tokenization / URL parsing)
  -> Rule Engine (ScamAnalysisService: deterministic, explainable)
  -> ML/AI Analysis (FraudIntelligenceService: learned naive-Bayes estimate)
  -> Risk Aggregator (FraudDetectionService: combines signals)
  -> Risk Score + Severity (LOW / MODERATE / HIGH / CRITICAL)
  -> Indicators + Recommendation (explainable output)
```

The two analysis engines are complementary (Section 17 of the blueprint):

- **Rules** — fast, deterministic, explainable for known scam patterns.
- **Learned model** — a Laplace-smoothed naive-Bayes log-odds estimator over
  confirmed-high analyses and community reports (`scam` corpus) vs
  low/moderate analyses (`benign` corpus).

## 5. Behavioural anomaly detection

`AnomalyService` scores a transaction against the user's own transaction
history using a z-score style deviation. Signals include unusually large
amounts and deviation from the user's historical mean/spread. Output is an
anomaly risk level plus a human-readable explanation.

## 6. Module map (frontend pages)

Dashboard, Spending (Transactions), Scam Scanner + Fraud History, Transaction
Safety, Decision Safety, What-If Simulator, Incidents, Alerts, Education,
Investments, Products, Markets, Budget, Goals, Profile, Financial Health,
Comparison, Assistant (chat), Login/Register.

## 7. Deployment topology

```text
        INTERNET
            |
    +-------+-------+
    |               |
    v               v
Frontend host    Backend host (Spring Boot)
(static SPA)      |
                  v
           Managed PostgreSQL
```

See `docs/security/security.md` and the deployment section of `api-documentation.md`.
