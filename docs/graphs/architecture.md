# Architecture

> Topic: Financial Fraud Assistant system architecture
> Source: backend/src/main/java, frontend/src, live API surface
> Date: 2026-08-30

```mermaid
flowchart LR
  subgraph FE["Frontend (React + Vite)"]
    P[Pages: Dashboard, Spending, ScamScanner, Assistant, TransactionSafety, DecisionSafety, WhatIf, Incidents, Alerts, Education, Investments, Products, Markets, Budget, Goals, Profile, FinancialHealth, Compare, FraudHistory, Login/Register]
    A[api.js: axios base /api + JWT header]
  end

  subgraph SEC["Security layer"]
    FC[JwtAuthenticationFilter - validates Bearer]
    SC[SecurityConfig - stateless, JSON 401/403]
    CORS[allowed origin localhost:5173]
  end

  subgraph CTRL["Controllers - all scope by requireUser (IDOR-safe)"]
    AuthC[AuthController]
    TxC[TransactionController]
    FpC[FinancialProfileController]
    FhC[FinancialHealthController]
    DcC[DashboardController]
    FrC[FraudScannerController / FraudController]
    Dc[DecisionController] & Sim[SimulatorController]
    AsC[AssistantController]
    EdC[EducationController]
    Inc[IncidentsController]
    Pr[Product / Market / Investment]
  end

  subgraph SVC["Services"]
    AuthS[AuthService + JwtService]
    TxS[TransactionService + AnomalyService]
    ScamS[ScamAnalysisService - 29 text rules + URL analysis]
    FdS[FraudDetectionService]
    HSS[HealthScoreService]
    Das[DashboardService]
    WiS[WhatIfService / InvestmentService]
  end

  subgraph DB["PostgreSQL 17"]
    U[users] T[transactions] P2[financial_profile] B[budgets]
    G[financial_goals] FA[fraud_analysis] FI[fraud_indicators]
    AL[alerts] SR[scam_reports] CC[chat_conversations]
    EM[education_modules] QA[quiz_questions] EA[education_attempts]
    PROD[financial_products]
  end

  FE -->|/api/*| SEC --> CTRL --> SVC --> DB
  ERR[GlobalExceptionHandler - ApiError 400/401/403/404/405/413/500] -.applies to.-> CTRL
```