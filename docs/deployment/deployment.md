# FinanceSafe — Deployment Guide

> Local + production deployment instructions for the SIH demo.

---

## 1. Local development stack

| Component | Command | URL |
| --------- | ------- | --- |
| PostgreSQL 17 | `docker compose up -d` | `localhost:5432` |
| Backend (Spring Boot) | `mvn spring-boot:run` (from `backend/`) | `http://localhost:8080` |
| Frontend (React + Vite) | `npm install && npm run dev` (from `frontend/`) | `http://localhost:5173` |

Health check: `GET http://localhost:8080/api/health` → `{ "status": "UP", ... }`.

`docker-compose.yml` also mounts `database/schema` into
`/docker-entrypoint-initdb.d` so a fresh database is initialised on first run.

## 2. Environment variables

Copy templates and fill in real values (never commit secrets):

- `backend/.env.example` → backend env
- `frontend/.env.example` → frontend env
- root `.env.example` → convenience aggregate

### Backend

| Variable | Default (dev) | Production |
| -------- | ------------- | ---------- |
| `SERVER_PORT` | `8080` | 8080 / platform-assigned |
| `DB_URL` | `jdbc:postgresql://localhost:5432/financial_fraud_assistant` | managed Postgres JDBC URL |
| `DB_USERNAME` | `postgres` | managed DB user |
| `DB_PASSWORD` | `postgres` | managed DB password |
| `JWT_SECRET` | development value | **long random secret** |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | production frontend URL |

### Frontend

| Variable | Value |
| -------- | ----- |
| `VITE_API_BASE_URL` | deployed backend API URL (e.g. `https://api.example.com/api`) |

## 3. CORS

The backend reads `CORS_ALLOWED_ORIGIN` (default `http://localhost:5173`).
Set it to the exact production frontend origin during deployment. Allowed
methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`; allowed headers:
`Authorization, Content-Type`.

## 4. Deployment topology

```text
        INTERNET
            |
    +-------+-------+
    |               |
    v               v
Frontend host    Backend host           Managed PostgreSQL
(static SPA,     (Spring Boot,          (database-as-a-service)
 Vercel/Netlify)  Render/Railway)
                            |
                            v
                 Oracle/Temurin JRE 17
                 Java -jar backend.jar
```

### Build the backend

```bash
cd backend
mvn -DskipTests package        # produces target/backend-<version>.jar
java -jar target/backend-*.jar
```

### Run (illustrative, Render/Railway-style)

```bash
export DB_URL=jdbc:postgresql://<host>:5432/<db>?sslmode=require
export DB_USERNAME=<user>
export DB_PASSWORD=<password>
export JWT_SECRET=<long-random-secret>
export CORS_ALLOWED_ORIGIN=https://<frontend-domain>
java -jar backend-*.jar
```

The same environment variables map directly to a PaaS dashboard.

## 5. Smoke tests (post-deploy)

1. `GET /api/health` → 200 `UP`.
2. `POST /api/auth/register` → 201 with a JWT.
3. `POST /api/auth/login` → 200 with a JWT.
4. Call a protected endpoint with the JWT (e.g. `GET /api/dashboard`) → 200.
5. Call a protected endpoint **without** a JWT → 401.
6. From the production frontend origin, confirm CORS works (browser requests succeed).

## 6. Notes

- `spring.jpa.hibernate.ddl-auto=update` manages schema for convenience; for a
  hardened release, prefer explicit migrations (`database/schema`).
- Do not deploy with the development `JWT_SECRET`.
- The ML stage runs in-JVM; no Python service is required for the MVP.
