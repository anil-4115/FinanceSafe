# 01 — Authentication

Automated coverage: ✅ `AuthIntegrationTest` (5), `AuthServiceTest` (5), `JwtServiceTest` (5)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 1.1 | Register new user | `POST /api/auth/register` `{fullName,email,password}` | 201 + `token` + user | ✅ PASS |
| 1.2 | Duplicate email register | register same email twice | 2nd → 4xx (conflict) | ✅ PASS (AuthIntegrationTest) |
| 1.3 | Weak password rejected | register with `<8` char password | 400 VALIDATION_ERROR | ✅ PASS |
| 1.4 | Login correct credentials | `POST /api/auth/login` | 200 + `token` | ✅ PASS |
| 1.5 | Login wrong password | login bad password | 401 | ✅ PASS |
| 1.6 | Protected route no token | `GET /api/dashboard` no header | 401 | ✅ PASS — 401 |
| 1.7 | Protected route with token | `GET /api/dashboard` + Bearer | 200 | ✅ PASS |
| 1.8 | Tampered JWT | send modified token | 401 | ✅ PASS |
| 1.9 | Expired JWT | send expired token | 401 | ✅ PASS |
| 1.10 | Password never returned | inspect register/login JSON | no `passwordHash` | ✅ PASS |
