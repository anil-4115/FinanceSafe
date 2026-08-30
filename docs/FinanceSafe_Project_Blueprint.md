# FinanceSafe --- Financial Fraud Assistant

## Complete Project Blueprint, Architecture & Implementation Roadmap

> **Purpose:** Single source of truth for building, testing,
> documenting, demonstrating and deploying the Financial Fraud Assistant
> / FinanceSafe SIH project.

------------------------------------------------------------------------

## 1. Project Overview

**FinanceSafe** is an intelligent financial safety platform that helps
users detect suspicious scams, analyze unusual transactions, understand
risk, and take safer actions.

### Core goals

-   Detect suspicious scam messages, links and financial requests.
-   Detect unusual transaction behaviour.
-   Calculate risk scores and severity levels: LOW, MODERATE, HIGH,
    CRITICAL.
-   Explain *why* an item is risky.
-   Provide actionable safety recommendations.
-   Protect user data with authentication, JWT, authorization and IDOR
    protection.
-   Provide dashboards, alerts, transaction history and
    security/financial health information.

------------------------------------------------------------------------

## 2. Technology Stack

### Frontend

-   React
-   Vite
-   JavaScript
-   HTML/CSS
-   REST API integration

Local frontend configuration:

``` env
VITE_API_BASE_URL=http://localhost:8080/api
```

### Backend

**Spring Boot** is the recommended backend for the current
implementation.

It provides:

-   REST APIs
-   Authentication and authorization
-   JWT/Spring Security integration
-   Business logic
-   Database access
-   Fraud rule engine
-   Alerts and health-score logic

**Do not rewrite the existing Spring Boot backend just to change
frameworks.** It is already part of the working project.

### AI/ML

Use Python for specialized intelligence where useful:

-   scikit-learn
-   pandas
-   NumPy
-   NLP/model libraries as required

### Database

**PostgreSQL**

### Development

-   Maven
-   npm
-   Git/GitHub
-   VS Code

------------------------------------------------------------------------

# 3. Recommended Architecture

The recommended architecture is a **Modular Monolith + Specialized AI
Service**.

A full microservices architecture is unnecessary for the current SIH MVP
because it increases deployment and debugging complexity.

``` text
                         USER
                           |
                           v
                 +--------------------+
                 |   React + Vite      |
                 |    Frontend         |
                 +---------+----------+
                           |
                       REST/JSON
                           |
                           v
             +-----------------------------+
             |      Spring Boot Backend    |
             |                             |
             | Auth/JWT                    |
             | Fraud Scanner               |
             | Rule Engine                 |
             | Anomaly Integration         |
             | Health Score                |
             | Transactions                |
             | Alerts                      |
             | Security / Authorization    |
             +-------------+---------------+
                           |
              +------------+------------+
              |            |            |
              v            v            v
        PostgreSQL    Python AI/ML   External APIs
                       (optional)     (optional)
```

### Layer responsibilities

**React:** UI, forms, dashboards, visualizations and API calls.

**Spring Boot:** central application layer, security, REST APIs,
business logic, rule engine, risk aggregation and database access.

**Python AI/ML:** specialized fraud classification, anomaly detection,
NLP and behavioural analysis.

**PostgreSQL:** persistent application data.

------------------------------------------------------------------------

# 4. Main Features

## 4.1 Authentication

-   Registration
-   Login
-   Password hashing
-   JWT authentication
-   Protected routes/endpoints
-   Logout/session handling
-   Unauthorized-access handling

Flow:

``` text
User -> Login -> Validate credentials -> JWT
     -> Frontend session -> Bearer JWT -> Protected APIs
```

## 4.2 Scam/Fraud Scanner

Users can submit:

-   SMS text
-   WhatsApp-style messages
-   Email text
-   Suspicious links
-   KYC messages
-   Payment requests

Detection pipeline:

``` text
Input
  -> Validation
  -> Feature extraction
  -> Rule Engine
  -> AI/ML analysis
  -> Risk Aggregator
  -> Risk Score + Severity
  -> Indicators + Recommendation
```

### Scam categories

**KYC scams**

-   KYC update request
-   Account-blocking threat
-   Identity-document request
-   Banking credential request
-   Suspicious verification link

**OTP scams**

-   OTP request
-   Verification-code request
-   "Share the OTP" patterns
-   Urgent verification

**Impersonation**

-   Fake bank representative
-   Fake government official
-   Fake company/customer-care representative

**Urgency**

-   "Act now"
-   "Immediately"
-   "Account will be blocked"
-   "Last warning"
-   Short deadlines

**Lottery/prize scams**

-   Lottery win
-   Prize claim
-   Reward
-   Processing fee
-   Advance payment

**Suspicious payments**

-   Unknown beneficiary
-   Advance transfer
-   Payment before verification
-   Unusual payment request

**Suspicious URLs**

-   Unknown domains
-   Lookalike domains
-   Credential collection links
-   Suspicious URL patterns

------------------------------------------------------------------------

# 5. Risk Scoring

The scanner should return both a score and a severity.

``` text
0–24    LOW
25–49   MODERATE
50–74   HIGH
75–100  CRITICAL
```

These thresholds can be tuned after testing.

Example:

``` text
CRITICAL — 92/100

Indicators:
- KYC credentials requested
- OTP requested
- Urgent account-blocking threat
- Suspicious URL
- Impersonation detected

Recommended action:
Do not click the link or share credentials/OTP.
Verify through the official channel.
```

The scoring system should reward **meaningful evidence**, not
artificially inflate scores.

------------------------------------------------------------------------

# 6. Explainability

A key differentiator is:

> **Detect + Explain + Score + Protect**

Do not only show:

``` text
Fraud probability: 92%
```

Show:

``` text
CRITICAL — 92/100

Why?
1. KYC request
2. OTP request
3. Urgency
4. Suspicious URL
5. Impersonation

What to do?
- Do not share OTP.
- Do not click the link.
- Do not transfer money.
- Verify using the official channel.
```

This makes the system understandable to normal users and stronger in an
SIH demonstration.

------------------------------------------------------------------------

# 7. Anomaly Detection

The anomaly engine identifies transactions that differ from a user's
normal behaviour.

Possible signals:

-   Unusually large amount
-   New beneficiary
-   Unusual transaction time
-   Unusual frequency
-   Sudden spending increase
-   Deviation from historical behaviour
-   Other available behavioural signals

Example:

``` text
Normal transactions: ₹500–₹2,000

New transaction: ₹85,000 to a new beneficiary

Result: HIGH ANOMALY RISK
```

The system should explain the reason for the anomaly.

------------------------------------------------------------------------

# 8. Financial/Security Health Score

Provide a simple overall security view.

Possible inputs:

-   Suspicious transactions
-   Fraud alerts
-   Scam scan history
-   Unusual activity
-   Security status
-   Other available security indicators

Example:

``` text
Security Health Score
82 / 100
GOOD
```

Display:

-   Current score
-   Major reasons for the score
-   Changes over time
-   Recommendations

------------------------------------------------------------------------

# 9. Transactions

Users can:

-   View transactions
-   Filter transactions
-   Inspect transaction details
-   See anomaly/risk status
-   Review transaction history

Suggested fields:

``` text
id
user_id
amount
beneficiary
transaction_type
transaction_time
status
risk_score
anomaly_status
created_at
```

------------------------------------------------------------------------

# 10. Alerts

Alerts can be generated for:

-   High-risk transaction
-   Critical scam scan
-   New anomaly
-   Suspicious login
-   Security warning

Suggested fields:

``` text
id
user_id
type
severity
message
created_at
read
```

------------------------------------------------------------------------

# 11. Dashboard

The dashboard should provide a quick overview:

``` text
+----------------------------------+
| Security Health Score: 82/100    |
+----------------------------------+

+---------------+------------------+
| Fraud Alerts  | Suspicious       |
|       4       | Activity: 2      |
+---------------+------------------+

+----------------------------------+
| Recent Transactions              |
+----------------------------------+

+----------------------------------+
| Recent Security Alerts           |
+----------------------------------+
```

------------------------------------------------------------------------

# 12. Security Architecture

Financial data requires strong access control.

## JWT

Protected requests use:

``` text
Authorization: Bearer <JWT>
```

## Authorization

Every protected resource must be checked against the authenticated
user's identity.

## IDOR protection

Never trust an ID supplied by the client.

Correct flow:

``` text
Request resource
  -> Read user identity from JWT
  -> Find resource
  -> Verify ownership/permission
  -> Return resource
```

If User A changes an ID to User B's resource, the API must not expose
User B's data.

## Other controls

-   Password hashing
-   Input validation
-   CORS configuration
-   Secure error responses
-   ORM/prepared queries
-   Environment variables for secrets
-   Minimal sensitive logging
-   Role/permission checks where required

------------------------------------------------------------------------

# 13. Database Design

Core tables can include:

``` text
users
transactions
fraud_scans
fraud_indicators
alerts
health_scores
audit_logs
```

### Users

``` text
id
name
email
password_hash
role
created_at
updated_at
```

### Transactions

``` text
id
user_id
amount
beneficiary
transaction_type
transaction_time
status
risk_score
anomaly_status
created_at
```

### Fraud scans

``` text
id
user_id
input_type
input/reference
risk_score
severity
created_at
```

Only store sensitive/raw data when necessary.

------------------------------------------------------------------------

# 14. Backend Organization

Logical modules:

``` text
backend/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── .../
    │   │       ├── auth/
    │   │       ├── security/
    │   │       ├── fraud/
    │   │       ├── anomaly/
    │   │       ├── transaction/
    │   │       ├── alert/
    │   │       ├── health/
    │   │       ├── user/
    │   │       └── common/
    │   └── resources/
    └── test/
```

Follow the existing package structure where possible; do not restructure
unnecessarily.

------------------------------------------------------------------------

# 15. Frontend Organization

``` text
frontend/
├── public/
├── src/
│   ├── components/
│   ├── pages/
│   ├── services/
│   ├── hooks/
│   ├── context/
│   ├── utils/
│   └── assets/
├── .env
├── package.json
└── vite.config.js
```

Centralize API communication where practical.

``` text
React pages
    |
    v
API service layer
    |
    v
Spring Boot REST API
```

------------------------------------------------------------------------

# 16. AI/ML Organization

``` text
ai/
├── fraud_model/
├── anomaly_model/
├── feature_engineering/
├── preprocessing/
├── api/
└── requirements.txt
```

Possible AI endpoints:

``` text
POST /predict-fraud
POST /detect-anomaly
POST /analyze-text
```

Use the actual implemented API names in the final documentation.

------------------------------------------------------------------------

# 17. Hybrid Fraud Intelligence

The strongest approach is:

``` text
              Input
                |
       +--------+--------+
       |                 |
       v                 v
   Rule Engine         AI/ML
       |                 |
       +--------+--------+
                |
                v
         Risk Aggregator
                |
        +-------+-------+
        |               |
        v               v
    Risk Score      Explanation
```

### Why hybrid?

**Rules:** fast, deterministic and explainable for known scam patterns.

**AI/ML:** useful for complex patterns, anomaly detection, text
classification and future expansion.

------------------------------------------------------------------------

# 18. API Design

Typical API groups:

``` text
/api/auth/*
/api/users/*
/api/fraud/*
/api/anomaly/*
/api/transactions/*
/api/alerts/*
/api/health/*
```

Examples:

``` text
POST /api/auth/register
POST /api/auth/login

POST /api/fraud/scan
GET  /api/fraud/history

GET  /api/transactions
GET  /api/transactions/{id}

GET  /api/alerts
PUT  /api/alerts/{id}/read

GET  /api/health-score
```

The exact paths must match the existing implementation.

------------------------------------------------------------------------

# 19. Local Development

## Backend

``` powershell
cd "D:\Collegeinanical-fraud-assitantinancial-fraud-assistantackend"
mvn spring-boot:run
```

Normally:

``` text
http://localhost:8080
```

## Frontend

Open another terminal:

``` powershell
cd "D:\Collegeinanical-fraud-assitantinancial-fraud-assistantrontend"
npm run dev
```

Normally:

``` text
http://localhost:5173
```

## PostgreSQL

Normally:

``` text
Host: localhost
Port: 5432
```

A simple database connectivity check is:

``` powershell
& "C:\Program Files\PostgreSQLin\psql.exe" -U postgres -h localhost -p 5432 -c "SELECT 1 AS ok;"
```

This only checks PostgreSQL connectivity. It does not start the frontend
or backend.

------------------------------------------------------------------------

# 20. Testing Strategy

## Unit tests

Test:

-   Authentication
-   JWT
-   Fraud rules
-   Risk scoring
-   Anomaly logic
-   Health score
-   Alerts
-   Validation

## Integration tests

Test:

-   API + database
-   Login flow
-   Protected APIs
-   Fraud scan API
-   Transaction API
-   Alert API

## Security tests

Important cases:

``` text
No JWT              -> 401
Invalid JWT         -> 401
Expired JWT         -> 401
Unauthorized user  -> 403/404
IDOR attempt        -> Must not expose data
```

## Frontend tests

For every important page verify:

1.  Page opens.
2.  Correct API is called.
3.  Loading state works.
4.  Success state works.
5.  Empty state works.
6.  Error state works.
7.  Authentication works.
8.  Navigation works.

------------------------------------------------------------------------

# 21. Scam Scanner Test Suite

Create fixed demo cases.

### Case 1 --- Safe

``` text
Your monthly statement is ready.
Please sign in using your normal banking application.
```

Expected:

``` text
LOW
```

### Case 2 --- KYC scam

Include:

-   KYC request
-   Urgency
-   Account blocking
-   Suspicious link
-   Credential request

Expected:

``` text
CRITICAL
5+ meaningful indicators
```

### Case 3 --- OTP scam

Include:

-   OTP request
-   Urgency
-   Impersonation
-   Payment/account request

Expected:

``` text
HIGH/CRITICAL
```

### Case 4 --- Lottery scam

Include:

-   Prize
-   Winner claim
-   Processing fee
-   Payment request

Expected:

``` text
HIGH/CRITICAL
```

### Case 5 --- Impersonation

Include:

-   Fake official identity
-   Threat
-   Urgency
-   Credential/payment request

Expected:

``` text
HIGH/CRITICAL
```

------------------------------------------------------------------------

# 22. Current Priority: Scam Scanner Quality

The KYC demo should produce the intended result.

Target:

``` text
CRITICAL
5+ meaningful indicators
```

Implementation steps:

1.  Study the current `ScamAnalysisService`/rule engine.
2.  List current indicators and weights.
3.  Add missing KYC, OTP, urgency, impersonation and URL rules.
4.  Prevent duplicate counting of the same evidence.
5.  Tune risk weights.
6.  Validate severity thresholds.
7.  Run fixed test cases.
8.  Compare expected vs actual results.

Do not inflate scores without meaningful evidence.

------------------------------------------------------------------------

# 23. SIH Differentiation

Do not position the project as only:

> "An AI fraud detector."

Position it as:

> **An explainable financial safety assistant combining scam
> intelligence, behavioural anomaly detection, risk scoring, security
> health monitoring and actionable protection guidance.**

Key differentiators:

1.  **Explainable detection** --- tells the user why something is risky.
2.  **Hybrid intelligence** --- rules + AI/ML.
3.  **Multi-signal scoring** --- combines several indicators.
4.  **Behavioural anomaly detection** --- detects unusual transaction
    patterns.
5.  **Security/financial health score** --- summarizes overall safety.
6.  **Security-by-design** --- JWT, authorization and IDOR protection.
7.  **Actionable recommendations** --- tells users what to do next.

------------------------------------------------------------------------

# 24. Documentation Plan

Recommended `docs/` structure:

``` text
docs/
├── architecture/
│   └── architecture.md
├── api/
│   └── api-documentation.md
├── database/
│   └── database-design.md
├── sih/
│   ├── problem-solution.md
│   ├── innovation.md
│   ├── demo-story.md
│   └── judging-points.md
└── baseline-checkpoint.md
```

Document:

-   Problem
-   Proposed solution
-   Architecture
-   Features
-   APIs
-   Database
-   AI/ML
-   Security
-   Testing
-   Deployment
-   SIH innovation
-   Demo flow

------------------------------------------------------------------------

# 25. SIH Demo Story

Use one realistic end-to-end story.

## Step 1 --- Login

User logs into FinanceSafe.

## Step 2 --- Dashboard

Show health score, alerts and recent activity.

## Step 3 --- Scam message

Paste:

``` text
Your bank account will be blocked today.
Complete KYC immediately:
http://suspicious-link.example
Send your OTP to verify.
```

## Step 4 --- Detection

Show:

``` text
CRITICAL
Risk Score: 90+
```

Indicators:

``` text
✓ KYC request
✓ OTP request
✓ Urgency
✓ Account-blocking threat
✓ Suspicious URL
```

## Step 5 --- Explanation

Show why the message is dangerous and the recommended actions.

## Step 6 --- Transaction anomaly

Open a transaction significantly outside the user's normal behaviour.

Show:

``` text
HIGH ANOMALY RISK
```

Explain the unusual amount/new beneficiary/deviation.

## Step 7 --- Alert

Show the generated security alert on the dashboard.

## Step 8 --- Security

Demonstrate that changing a resource ID cannot expose another user's
data.

## Step 9 --- Closing statement

> **FinanceSafe does not merely detect fraud --- it explains risk and
> helps users make safer financial decisions.**

------------------------------------------------------------------------

# 26. Deployment Architecture

``` text
                  INTERNET
                     |
          +----------+----------+
          |                     |
          v                     v
   Frontend Hosting       Backend Hosting
    Vercel/Netlify        Render/Railway
                                |
                                v
                       Managed PostgreSQL
                                |
                                v
                         Python AI Service
                           if needed
```

Potential choices:

-   Frontend: Vercel or Netlify
-   Backend: Render or Railway
-   Database: managed PostgreSQL
-   AI service: suitable Python hosting

The final provider should be selected based on current limits,
reliability and simplicity.

------------------------------------------------------------------------

# 27. Environment Variables

Never commit secrets.

Frontend:

``` env
VITE_API_BASE_URL=http://localhost:8080/api
```

Production:

``` env
VITE_API_BASE_URL=<DEPLOYED_BACKEND_API>
```

Backend values may include:

``` env
DATABASE_URL=<DATABASE_URL>
DATABASE_USERNAME=<DATABASE_USERNAME>
DATABASE_PASSWORD=<DATABASE_PASSWORD>
JWT_SECRET=<STRONG_SECRET>
AI_SERVICE_URL=<AI_SERVICE_URL>
```

Use the project's existing Spring configuration style.

Keep:

``` text
.env
```

out of Git and provide:

``` text
.env.example
```

for required variable names.

------------------------------------------------------------------------

# 28. Complete Development Roadmap

## Phase 1 --- Foundation

-   [x] Project structure
-   [x] Frontend setup
-   [x] Backend setup
-   [x] PostgreSQL setup
-   [x] Environment configuration
-   [x] Backend health check

## Phase 2 --- Core Application

-   [x] Authentication foundation
-   [x] JWT
-   [x] Main APIs
-   [x] Frontend pages
-   [x] Database integration

## Phase 3 --- Frontend/API Verification

-   [x] Verify frontend pages
-   [x] Verify corresponding APIs
-   [x] Fix UI/API mismatches
-   [x] Confirm local frontend/backend communication

## Phase 4 --- Fraud Intelligence

-   [ ] Study current scam rule engine
-   [ ] Improve KYC scam detection
-   [ ] Verify OTP rules
-   [ ] Verify urgency rules
-   [ ] Verify impersonation rules
-   [ ] Verify lottery/prize rules
-   [ ] Verify suspicious URL rules
-   [ ] Tune risk weights
-   [ ] Validate LOW/MODERATE/HIGH/CRITICAL
-   [ ] Verify all demo cases

## Phase 5 --- AI/Anomaly Validation

-   [ ] Validate anomaly detection
-   [ ] Test normal vs abnormal transactions
-   [ ] Validate AI responses
-   [ ] Connect AI service where required
-   [ ] Add meaningful explanations

## Phase 6 --- Backend + Security Testing

-   [ ] Unit tests
-   [ ] Integration tests
-   [ ] Authentication tests
-   [ ] JWT tests
-   [ ] Fraud scanner tests
-   [ ] Anomaly tests
-   [ ] Health-score tests
-   [ ] Alert tests
-   [ ] IDOR tests
-   [ ] Authorization tests
-   [ ] Input validation tests
-   [ ] Frontend regression testing

## Phase 7 --- SIH Documentation

-   [ ] Architecture
-   [ ] API documentation
-   [ ] Database documentation
-   [ ] AI/ML documentation
-   [ ] Security documentation
-   [ ] Innovation/differentiation
-   [ ] Demo story
-   [ ] Screenshots
-   [ ] Presentation content

## Phase 8 --- Deployment

-   [ ] Production environment variables
-   [ ] Production PostgreSQL
-   [ ] Backend deployment
-   [ ] AI service deployment if needed
-   [ ] Frontend deployment
-   [ ] CORS
-   [ ] Production API URL
-   [ ] Smoke tests

## Phase 9 --- Final Demo

-   [ ] Register/login
-   [ ] Dashboard
-   [ ] Scam scanner
-   [ ] Critical KYC demo
-   [ ] Anomaly demo
-   [ ] Health score
-   [ ] Alerts
-   [ ] Security/IDOR demonstration
-   [ ] End-to-end test
-   [ ] Backup demo data
-   [ ] Final rehearsal

------------------------------------------------------------------------

# 29. Priority From the Current Project State

Frontend/API verification is already completed.

Therefore:

``` text
CURRENT
   |
   v
1. Scam Scanner Quality
   |
   v
2. Backend Automated Tests
   |
   v
3. Security Testing
   JWT / IDOR / Authorization
   |
   v
4. AI / Anomaly Validation
   |
   v
5. SIH Documentation
   |
   v
6. Deployment
   |
   v
7. Final End-to-End Demo
```

Do **not** spend time rewriting the architecture unless a real technical
blocker appears.

------------------------------------------------------------------------

# 30. Definition of Done

## Frontend

-   All major pages open.
-   Navigation works.
-   API data loads.
-   Loading/error/empty states work.

## Backend

-   Backend starts reliably.
-   Health endpoint works.
-   APIs return correct responses.
-   Validation works.

## Authentication

-   Registration works.
-   Login works.
-   JWT works.
-   Protected endpoints reject unauthenticated requests.

## Security

-   Users cannot access other users' data.
-   IDOR tests pass.
-   Secrets are not hard-coded.
-   Input validation works.

## Fraud Scanner

-   Safe content -\> LOW.
-   Moderate scam -\> MODERATE.
-   Strong scam -\> HIGH.
-   KYC demo -\> CRITICAL with 5+ meaningful indicators.
-   Explanation is displayed.

## Anomaly Detection

-   Normal transaction behaves normally.
-   Clearly unusual transaction is detected.
-   Explanation is displayed.

## Health Score

-   Score is displayed.
-   Calculation is consistent.
-   Recommendations are meaningful.

## Deployment

-   Frontend reaches backend.
-   Backend reaches PostgreSQL.
-   Production variables are configured.
-   CORS works.
-   End-to-end flow works.

------------------------------------------------------------------------

# 31. Final Product Flow

``` text
USER
  |
  v
FinanceSafe UI
  |
  v
Authentication
  |
  v
Secure API Layer
  |
  +------------+-------------+-------------+
  |            |             |             |
  v            v             v             v
Scam       Transactions   Dashboard     Alerts
Scanner        |             |
  |            v             v
  |       Anomaly Engine  Health Score
  |            |             |
  +------------+-------------+
               |
               v
          AI / ML Analysis
               |
               v
         Risk Aggregator
               |
        +------+------+
        |             |
        v             v
   Risk Score    Explanation
        |             |
        +------+------+
               |
               v
             USER
```

------------------------------------------------------------------------

# 32. Architecture Statement for SIH

Use this in the presentation:

> **FinanceSafe follows a secure modular architecture with React as the
> presentation layer, Spring Boot as the central API and business-logic
> layer, PostgreSQL as the persistence layer, and Python-based AI/ML
> services for specialized fraud and anomaly analysis. A hybrid
> rule-engine and AI approach provides fast, explainable fraud
> detection, while JWT authentication, authorization and IDOR protection
> secure user data.**

------------------------------------------------------------------------

# 33. Final Vision

FinanceSafe should answer four questions:

``` text
1. Is this suspicious?
2. Why is it suspicious?
3. How serious is the risk?
4. What should the user do now?
```

The product philosophy is:

> **DETECT → EXPLAIN → SCORE → PROTECT**
