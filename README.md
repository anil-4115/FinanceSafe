# Financial Fraud Assistant

A fintech + fraud safety platform for SIH 2026.

## Project architecture

- frontend: React + Vite + Tailwind
- backend: Java + Spring Boot + PostgreSQL
- ai: Python-based fraud and finance analysis modules
- docs: project and demo documentation

## Current scope

FinanceSafe is being built as a Smart India Hackathon demo with a real-product workflow:

- user accounts and financial profiles
- manual transaction entry and CSV bank-statement import
- fraud signals for unusual amounts, new merchants, transaction velocity, risky categories, and scam reports
- fraud alerts, budgets, financial goals, health score, and personalised recommendations

## Run locally

Prerequisites: Java 17, Maven, Node.js 20+ and Docker Desktop.

1. Start PostgreSQL:

   ```bash
   docker compose up -d
   ```

2. Start the API (from `backend`):

   ```bash
   ./mvnw spring-boot:run
   ```

   If Maven Wrapper is unavailable on Windows, use `mvn spring-boot:run`.
   Confirm it at `http://localhost:8080/api/health`.

3. Start the web app (from `frontend`):

   ```bash
   npm install
   npm run dev
   ```

   Open the URL Vite prints (normally `http://localhost:5173`).

Environment templates are included in `frontend/.env.example` and `backend/.env.example`. Do not commit real credentials or production JWT secrets.

## Authentication API

The frontend uses the following local endpoints:

- `POST /api/auth/register` with `fullName`, `email`, and an 8+ character `password`
- `POST /api/auth/login` with `email` and `password`

Both return a JWT bearer token and basic user information. The browser stores this session locally and sends the token in the `Authorization` header for protected API calls.

## Transaction CSV import

Use a UTF-8 CSV with these required headers: `date`, `merchant`, `amount`. Optional headers are `type`, `category`, and `notes`.

```csv
date,merchant,amount,type,category,notes
2026-08-01,Salary,75000,income,Salary,Monthly salary
2026-08-02,Local Grocery,-1450,expense,Food,Weekly groceries
```

Dates use `YYYY-MM-DD`. Negative amounts and `debit` rows are treated as expenses. The importer accepts up to 1,000 transaction rows per file and reports invalid rows without discarding valid ones.

## Fraud-safety signals

The current demo uses transparent rules rather than claiming opaque AI detection. It raises alerts for unusually large expenses, high-value payments to new merchants, high-risk terms/categories (for example crypto, gift cards, or gambling), and unusually rapid same-day manual activity. Users can also report phone, SMS, WhatsApp, email, UPI, and website scams. Scam reports are keyword-scored for indicators such as OTP/PIN requests, KYC traps, screen sharing, phishing, or gift-card demands.

These alerts are safety signals for review, not proof that a transaction is fraudulent. Users should contact their bank through official channels and, in India, promptly report suspected cyber fraud to 1930 or cybercrime.gov.in.
