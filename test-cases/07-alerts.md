# 07 — Alerts

Automated coverage: ✅ `FeatureCoverageIntegrationTest` (alerts_resolveOwnAndRejectOthers)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 7.1 | List alerts | `GET /api/alerts` | 200 list | |
| 7.2 | Alert created by activity | load sample data → GET alerts | at least 1 alert present | |
| 7.3 | Resolve own alert | `PATCH /api/alerts/{id}/resolve` | 200, status RESOLVED | |
| 7.4 | Resolve other user's alert | user B resolve user A's alert id | 404 | |
| 7.5 | Auth required | no token | 401 | |
