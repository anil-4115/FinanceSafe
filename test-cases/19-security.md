# 19 — Security & IDOR

Automated coverage: ✅ `SecurityIntegrationTest` (11), `ErrorHandlingIntegrationTest` (6)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | ----- | -------- |
| 19.1 | No JWT → protected | GET dashboard no token | 401 | |
| 19.2 | Tampered JWT | modified token | 401 | |
| 19.3 | Expired JWT | expired/wrong-secret token | 401 | |
| 19.4 | IDOR fraud history | user B read user A scan id | 404 / no data | |
| 19.5 | IDOR transaction | user B GET/PUT/DELETE user A txn | 404 / no data | |
| 19.6 | IDOR alert resolve | user B resolve user A alert | 404 | |
| 19.7 | Validation error shape | POST bad body | 400 + ApiError envelope, no stack trace | |
| 19.8 | Response strips secrets | incidents/profile/dashboard | no `passwordHash`, no emails | |
