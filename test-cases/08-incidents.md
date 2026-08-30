# 08 — Fraud reports / Incidents

Automated coverage: ✅ `FeatureCoverageIntegrationTest` (scamReport_thenIncidentsFeed) + `SecurityIntegrationTest`

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | ----- | -------- |
| 8.1 | Submit scam report | `POST /api/fraud/reports` | 201 `{riskScore}` | |
| 8.2 | List incidents | `GET /api/incidents` | 200 list (max 50, newest first), no identity field leaks | |
| 8.3 | Report validation | missing description | 400 | |
| 8.4 | Auth required for incidents | no token | 401 | |
