# 20 — Deployment Smoke

Automated coverage: ✅ backend health + integration tests; manual for hosts

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | ----- | -------- |
| 20.1 | Health endpoint | `GET /api/health` | 200 `UP` | |
| 20.2 | Frontend reaches backend | VITE_API_BASE_URL correct, CORS origin matches | browser requests succeed | |
| 20.3 | DB reachable | backend starts without DB errors | health UP, no connection errors | |
| 20.4 | Env overrides | DB_* / JWT_SECRET / CORS_ALLOWED_ORIGIN set | app uses them, no default-only secrets in prod | |
| 20.5 | Register→login→protected | full curl flow | 201→200→200 | |
