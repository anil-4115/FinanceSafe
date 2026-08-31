# 🚀 Deploy FinanceSafe to Supabase — Step-by-Step

> **What "upload to Supabase" means for this project:**
> FinanceSafe already ships its **own JWT auth + BCrypt password hashing**
> (Spring Security). So this guide uses **Supabase ONLY as a managed
> PostgreSQL database** — you do **NOT** use Supabase Auth / Storage / RLS
> for the app. No application code changes are required: the backend already
> reads its database settings from environment variables.

---

## Table of contents

1. [Architecture: local vs cloud DB](#1-architecture-local-vs-cloud-db)
2. [PART A — Create the Supabase database](#part-a--create-the-supabase-database)
3. [PART B — Convert the URI to a JDBC URL](#part-b--convert-the-uri-to-a-jdbc-url)
4. [PART C — Create the tables](#part-c--create-the-tables)
5. [PART D — Deploy the app and point it at Supabase](#part-d--deploy-the-app-and-point-it-at-supabase)
6. [PART E — Verification checklist](#part-e--verification-checklist)
7. [Environment variables reference](#environment-variables-reference)
8. [Troubleshooting](#troubleshooting)

---

## 1. Architecture: local vs cloud DB

| | Local (current dev) | Supabase (production) |
|---|--------------------|-----------------------|
| **Where PostgreSQL runs** | Docker container on your machine (`localhost:5432`) | Supabase's hosted servers |
| **Reachable from the internet** | ❌ No | ✅ Yes |
| **Stays online when laptop closes** | ❌ No | ✅ Yes |
| **Cost** | Free | Free tier + paid as you grow |

**Rule of thumb:** local Postgres is for development/demo. For a deployed
app you must use a **cloud-managed database** (Supabase in this case) because
a local database cannot be reached by your deployed app's users.

The app itself never needs its own copy of Postgres in production — you just
change the connection string (`DB_URL`) from local to Supabase.

```text
BEFORE (development)                     AFTER (production)
+-------------+   +-----------+          +-------------+   +-------------+
| Dock. PG    |<--| Backend   |          |  Backend    |-->|  Supabase   |
| localhost   |   | Spring    |          |  (Render/   |   |  Postgres   |
| :5432       |   | Boot      |          |  Railway)   |   |  (cloud)    |
+-------------+   +-----------+          +-------------+   +-------------+
```

---

## PART A — Create the Supabase database

1. **Sign up** at → <https://supabase.com> (GitHub or email). Free plan is fine.
2. Click **"New project"**:
   - **Name:** `financial-fraud-assistant` (any name)
   - **Database Password:** create a strong password → **save it now** (you
     cannot view it later)
   - **Region:** choose one near you (e.g. `Singapore` or `ap-south-1`)
   - Click **Create new project** and wait ~2 min while it provisions.
3. **Get the connection string:**
   - Left sidebar → **Project Settings → Database**
   - Find **"Connection string"** → switch to **URI** → it looks like:
     ```
     postgresql://postgres.<your-ref>:<DB-PASSWORD>@aws-0-<region>.pooler.supabase.com:6543/postgres
     ```
   - Click **Copy**.

---

## PART B — Convert the URI to a JDBC URL

Supabase gives a `postgresql://` URI, but Spring Boot needs the
`jdbc:postgresql://` format.

**URI (Supabase gives you):**
```
postgresql://postgres.abcd1234:MySecret@aws-0-us-east-1.pooler.supabase.com:6543/postgres
```

**JDBC (what the backend needs):**

| Env var | Value |
|---------|-------|
| `DB_URL` | `jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres` |
| `DB_USERNAME` | `postgres.abcd1234` (the `postgres.<your-ref>` part) |
| `DB_PASSWORD` | `MySecret` (the database password from Part A) |

> **Pooler vs direct connection:**
> - **Port 6543 (pooler)** — recommended for most hosts; username is `postgres.<ref>`.
> - **Port 5432 (direct)** — if pooler causes problems, use the direct
>   connection `jdbc:postgresql://db.<your-ref>.supabase.co:5432/postgres`
>   with username `postgres`.

---

## PART C — Create the tables

### Option C1 (recommended): let Hibernate create them automatically
The backend uses `spring.jpa.hibernate.ddl-auto=update`. On first startup
connected to Supabase it will auto-create **all tables**. **No action needed.**

### Option C2 (manual): run your SQL in Supabase
1. Supabase sidebar → **SQL Editor → New query**.
2. Open the schema files and paste their contents (in order):
   - `database/schema/01_init.sql`
   - `database/schema/02_extension.sql`
3. Click **Run** for each.
4. Either way, the DB is ready. If you used C1, tables appear after the first
   backend deploy (Part D).

---

## PART D — Deploy the app and point it at Supabase

> Supabase hosts only the **database**. Your Spring Boot backend + React
> frontend must run on a separate hosting platform. Two easy options:
> **Render** (recommended) or **Railway**. The steps below use Render, but the
> environment variables are identical on any host.

### D1 — Deploy the backend (Render Web Service)

1. Push this project to a GitHub repository.
2. At <https://render.com> → **New → Web Service** → pick your repo → choose
   the `backend/` directory.
3. **Runtime:** Docker. Render uses the existing `backend/Dockerfile`
   automatically.
4. **Environment variables** (set ALL of these):

   ```
   DB_URL=jdbc:postgresql://<supabase-host>:6543/postgres
   DB_USERNAME=postgres.<your-ref>
   DB_PASSWORD=<your-db-password>
   JWT_SECRET=<a-long-random-string-32+characters>
   CORS_ALLOWED_ORIGIN=<your-frontend-url>
   SERVER_PORT=8080
   ```

5. Click **Create Web Service** and wait for the first deploy (it builds the
   Maven image). Note the backend's public URL, e.g. `https://your-api.onrender.com`.

### D2 — Deploy the frontend (Render Static Site / Web Service)

1. **New → Static Site** → pick your repo → **Root directory:** `frontend`.
2. **Build command:** `npm install && npm run build`
3. **Publish directory:** `dist`
4. **Environment variable (build-time):**
   ```
   VITE_API_BASE_URL=<your-deployed-backend-url>/api
   ```
   (e.g. `https://your-api.onrender.com/api`)
5. Deploy and open the frontend URL.

> If you use the **frontend/Dockerfile** (Nginx) on a Web Service instead,
> pass `VITE_API_BASE_URL` as a build arg with the same value.

### D3 — Test locally against Supabase first (optional, before full deploy)

You can point your **local** backend at Supabase to confirm the connection
before deploying:

```powershell
# from backend\
$env:DB_URL="jdbc:postgresql://<supabase-host>:6543/postgres"
$env:DB_USERNAME="postgres.<your-ref>"
$env:DB_PASSWORD="<your-db-password>"
$env:JWT_SECRET="a-long-random-string-here-1234567890"
$env:CORS_ALLOWED_ORIGIN="http://localhost:5173"
mvn spring-boot:run
```

Run the frontend locally (`npm run dev`), register a user, then check it
appears in **Supabase → Table Editor → `users`**.

---

## PART E — Verification checklist

- [ ] Supabase project created, DB password saved.
- [ ] Got the connection URI and converted it to JDBC (`DB_URL`,
      `DB_USERNAME`, `DB_PASSWORD`).
- [ ] Tables created — via Hibernate (`ddl-auto=update`) or manually (SQL
      Editor). Verified `users` exists.
- [ ] Backend deployed with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
      `JWT_SECRET`, `CORS_ALLOWED_ORIGIN`.
- [ ] Frontend deployed with `VITE_API_BASE_URL` → your backend.
- [ ] Registered a test user and saw it appear in Supabase Table Editor →
      `users`.
- [ ] Logged in and confirmed a feature works end-to-end (e.g. health score,
      fraud scanner).

---

## Environment variables reference

| Variable | Where | Purpose | Example |
|----------|-------|---------|---------|
| `DB_URL` | backend | JDBC connection to Supabase | `jdbc:postgresql://host.pooler.supabase.com:6543/postgres` |
| `DB_USERNAME` | backend | Supabase DB user | `postgres.abcd1234` |
| `DB_PASSWORD` | backend | Supabase DB password | `MySecret` |
| `JWT_SECRET` | backend | Signs auth tokens (≥32 chars) | `d9f3...` |
| `CORS_ALLOWED_ORIGIN` | backend | Allowed browser origin | `https://yourapp.onrender.com` |
| `SERVER_PORT` | backend | Backend port (Render injects) | `8080` |
| `VITE_API_BASE_URL` | frontend (build) | Where the SPA calls the API | `https://yourapi.onrender.com/api` |

---

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| `Connection refused` at startup | Wrong Supabase host, or host not yet provisioned; verify the URL/port/username. |
| `FATAL: password authentication failed` | `DB_PASSWORD` / `DB_USERNAME` mismatch; for pooler the username includes `postgres.<ref>`. |
| Browser calls blocked (CORS) | `CORS_ALLOWED_ORIGIN` must equal the exact deployed frontend URL (no trailing slash). |
| `422`/`400` on some calls after deploy | Tables missing — let Hibernate create them or run `01_init.sql` + `02_extension.sql` in the Supabase SQL editor. |
| `Invalid signature` on login after redeploy | `JWT_SECRET` changed between runs, invalidating old tokens — log out and log in again. |
| Connection timeout on free tier | Free tier suspends the DB after ~1 week of inactivity; resume it in the Supabase dashboard. |
