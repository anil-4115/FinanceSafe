# FinanceSafe — Security Documentation

FinanceSafe is a **security-by-design** application. Financial data is guarded
by authentication, JSON Web Tokens, per-user authorization, IDOR protection,
input validation and safe error handling.

---

## 1. Authentication (JWT)

- **Stateless** JWT authentication (`SessionCreationPolicy.STATELESS`).
- On login/register, `AuthService` issues a signed JWT (`jwt.secret`).
- Clients send it on every request:
  `Authorization: Bearer <JWT>`.
- `JwtAuthenticationFilter` validates the token, loads the claims and populates
  the security context before a controller runs.
- Passwords are hashed with **BCrypt** (`BCryptPasswordEncoder`) — never stored
  in plain text.

### Auth lifecycle

```text
POST /api/auth/register  -> 201 + JWT
POST /api/auth/login     -> 200 + JWT
Protected APIs           -> require Authorization: Bearer <JWT>
```

## 2. Authorization & route protection

`SecurityConfig` permits only:

- `GET /api/health`
- `POST /api/auth/**`

every other `/api/**` route requires a valid JWT
(`anyRequest().authenticated()`).

Missing/invalid token → **401**; authenticated-but-not-allowed → **403**, both
returned as JSON via the exception handlers.

## 3. IDOR protection (data isolation)

Insecure Object Reference prevention is enforced in **every** user-scoped
service:

```text
Request resource
  -> read user identity from JWT (CurrentUserService.requireUser())
  -> query repository filtered by that user
  -> return only owned data
```

- `GET /api/fraud/history/{id}` returns **404** if the analysis is not owned
  by the current user.
- `PATCH /api/alerts/{alertId}/resolve` returns **404** if the alert is not
  owned by the current user.
- Transactions, budgets, goals, profile, chat and education attempts are all
  scoped by the current user.

Client-supplied IDs are never trusted on their own — identity always comes from
the validated JWT.

## 4. Input validation

- Bean Validation on all request DTOs (`@NotBlank`, `@Size`, etc.).
- Oversized payloads rejected (`413`).
- Values are parsed/sanitized before persistence (ORM/prepared queries via
  Spring Data JPA prevent SQL injection).

## 5. Secure error handling

- `GlobalExceptionHandler` produces a consistent `ApiError` envelope and never
  leaks stack traces or internal details.
- Security failures return JSON `{ success, message, errorCode, status }`.
- Sensitive fields (`passwordHash`, email) are excluded from API responses
  (verified by tests).

## 6. CORS

Restricted to the local frontend origin:

- Allowed origin: `http://localhost:5173`
- Methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`
- Headers: `Authorization, Content-Type`

Update the origin to the production frontend URL on deployment.

## 7. Secrets & environment

- No secrets are hard-coded for production.
- `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` come from environment
  variables (see `application.properties`).
- A strong `JWT_SECRET` **must** be set in production (the development default
  is only for local runs).

## 8. Automated security coverage

Covered by integration tests (`SecurityIntegrationTest`, `AuthIntegrationTest`):

| Case | Expected |
| ---- | -------- |
| No JWT | 401 |
| Invalid / tampered JWT | 401 |
| Expired JWT / wrong secret | 401 |
| Unauthorized access | 401/403 |
| IDOR (cross-user transaction/fraud/alert) | 404 / no data leaked |
| Incident feed | 401 alone; response strips identity fields |

## 9. Security checklist (Definition of Done)

- [x] Passwords BCrypt-hashed
- [x] No plain-text secrets committed
- [x] JWT required on protected routes
- [x] Every user-scoped query filtered by authenticated user
- [x] IDOR tests pass
- [x] Input validation applied
- [x] Consistent, non-leaking error responses
- [x] CORS restricted
