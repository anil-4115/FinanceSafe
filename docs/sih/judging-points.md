# FinanceSafe — Judging Points & Proof (SIH)

A quick reference map from likely judging criteria to the evidence in this
project.

---

## 1. Innovation & creativity

- **Explainable AI** — every detection lists weighted indicators + reasons +
  recommended actions (`FraudAnalysisResponse.indicators`,
  `FraudIntelligenceService.topSignals`).
- **Hybrid intelligence** — deterministic rule engine **plus** an in-JVM
  naive-Bayes learned model.
- **Behavioural anomaly detection** — per-user deviation analysis with
  explanations.
- **Security health score** — a single summarising safety metric.
- **Community incident feed** — aggregated scam reports (`/api/incidents`).

## 2. Technical complexity

- Spring Boot 3.3 + Spring Security + JWT + Hibernate/PostgreSQL.
- ~20 REST controllers; 15 JPA entities; layered services.
- Micro-features: CSV import, product comparison, market data, investment
  simulation, education + quiz, assistant chat, budgets & goals, what-if
  simulator.
- Learned-model training/scoring logic implemented from scratch.

## 3. Security & robustness

- JWT auth, BCrypt password hashing.
- Per-user authorization + **IDOR protection** verified by `SecurityIntegrationTest`.
- Input validation and consistent, non-leaking error envelopes.
- Environment-variable secrets; no production secrets in code.

## 4. Testing & quality

**64 automated tests, all passing.**

| Area | Tests |
| ---- | ----- |
| Scam scanner quality | 15 (`ScamAnalysisServiceTest`) |
| Authentication / JWT | 5 `AuthIntegrationTest` + 5 `JwtServiceTest` + 5 `AuthServiceTest` |
| Security (401/403/IDOR) | 11 `SecurityIntegrationTest` |
| Error handling / validation | 6 `ErrorHandlingIntegrationTest` |
| Anomaly detection | 3 `AnomalyServiceTest` |
| AI/ML responses | 4 `FraudIntelligenceServiceTest` |
| Health score | 5 `HealthScoreServiceTest` |
| Alert domain | 3 `AlertModelTest` |
| Financial flow | 3 `FinancialFlowIntegrationTest` |

## 5. Completeness

- Black-box scanner demo cases correctly classified:
  safe → LOW; KYC → CRITICAL (5+ indicators); OTP/lottery/impersonation →
  HIGH/CRITICAL.
- Health score, alerts, transactions, budgets, goals, education, products,
  markets all exposed and used by the frontend.

## 6. Social impact

- Targets the most common consumer frauds (KYC/OTP/impersonation/lottery).
- Educates users and promotes safer financial decision-making.
- Explainable, actionable guidance protects the most vulnerable users.

## 7. Demonstration readiness

- Sample-data loader (`/api/demo/load-sample`) for a clean start.
- A single end-to-end demo story (see `demo-story.md`).
- Fixed, repeatable scam cases so the demo is deterministic.
