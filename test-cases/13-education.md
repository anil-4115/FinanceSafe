# 13 — Education + Quiz

Automated coverage: ✅ `FeatureCoverageIntegrationTest` (education_modulesLiteracyDetailQuizAndAttempt)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 13.1 | List modules | `GET /api/education` | 200 list (non-empty, seeded) | |
| 13.2 | Literacy summary | `GET /api/education/literacy` | 200 | |
| 13.3 | Module detail | `GET /api/education/{id}` | 200 + content | |
| 13.4 | Quiz questions | `GET /api/education/{id}/quiz` | 200 + questions+options | |
| 13.5 | Submit attempt | `POST /api/education/{id}/attempt` | 200 + scorePct | |
| 13.6 | Quiz recorded | attempt reflected in literacy | 200 | |
| 13.7 | Auth required | no token | 401 | |
