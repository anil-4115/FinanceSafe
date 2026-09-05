# FIN-SHIELD Fix Work Log — Folder `4115`

> Ran by the AI assistant (opencode). Every bug discovery and fix action is recorded here as it happens.
> Project root: `frontend/` (Fin-SHIELD React app) against local backend `localhost:8080` (Supabase-backed, unchanged).
> Constraint: **frontend-only changes.** Backend/docker files must stay untouched.

## 0. Context
- Complete audit previously delivered (PASS/WARNING/ERROR/MISSING/BROKEN, file-by-file). Nothing had been changed during audit.
- User approved fixing everything: "Start fixing all this things".
- Verification environment: dev server `localhost:5173`, headless Edge CDP on `:9222`, QA login `qa-module-tester@test.com`.

## 1. Fix log

### 2026-09-05 — Fix batch 1 (BROKEN + ERROR + MISSING)
| # | What I did | File(s) | Verified? |
|---|---|---|---|
| B1 | Profile save broken: `const { notify } = useToast()` destructures the context function → `notify` undefined → save "succeeds" but catch swallows `TypeError` as "Could not save your profile." | `pages/Settings.jsx:14` | runtime probe showed misleading error + no toast |
| B2 | Markets page bricked when `/market/{symbol}` fails: `!symbol` list and `symbol && detail` both hidden, no reset. Fix: on failure reset `symbol=null`; add a loading placeholder `symbol && !detail`; clear stale error on successful search | `pages/Markets.jsx` | CDP detail-open verified |
| B3 | FraudHistory: detail-fetch failure replaced whole page with bare error. Fix: split `listError`/`detailError`, remove early-return, add list loading state, fix copy ("Fraud Scanner") | `pages/FraudHistory.jsx` | lint+build |
| E1 | Reports fabricated "All clear"/"0 open alerts"/"Fair" when a feed failed. Fix: track per-feed failures, render "Unavailable" instead of zeroes, add partial-report warning banner | `pages/Reports.jsx` | partial-render probe on kill one endpoint (TBD) |
| E2 | Compare link from product detail always 400 (single id vs 2–3 required). Fix: route to `/products?compare=<id>` and pre-select that product in Products compare mode | `pages/ProductDetails.jsx`, `pages/Products.jsx` | CDP click-through |
| M1 | Assistant never loaded saved chat history. Fix: `GET /assistant/history` on mount, append past messages after greeting | `pages/Assistant.jsx` | CDP traffic probe |
| D1 | Delete orphan files (no imports/routes): `Dashboard.jsx`, `Profile.jsx`, `Spending.jsx`, `ScamScanner.jsx`, `RiskBadge.jsx` | 5 files | `rg` import scan = 0 hits |

### 2026-09-05 — Fix batch 2 (WARNING batch A: core data pages)
| # | What I did | File(s) | Verified? |
|---|---|---|---|
| W1 | Transactions: `alive` guard on mount fetch; fresh `initialForm` date on each open; gate stats/charts ₹0-flash on loading; unanalyzed (riskScore null) excluded from "Normal", shown separately; clear stale error | `pages/Transactions.jsx` | lint+build |
| W2 | RiskAnalysis: add "Unanalyzed" bucket; partial-feed error banner instead of false "No open alerts" | `pages/RiskAnalysis.jsx` | lint+build |
| W3 | Overview: show tx data error instead of silent fail; guard `null` riskScore | `pages/Overview.jsx` | lint+build |
| W4 | TransactionDetails: reset stale loading/error on id change; robust reasons split; guard missing arrays | `pages/TransactionDetails.jsx` | lint+build |
| W5 | FinancialHealth: `health.strengths || []` guards; gate infinite skeleton on `health == null` | `pages/FinancialHealth.jsx` | lint+build |
| W6 | Alerts: single `useEffect` load, clear stale error, loading skeleton, `maxLength` guard | `pages/Alerts.jsx` | lint+build |

### 2026-09-05 — Fix batch 3 (WARNING batch B: finance/forms + CSS)
| # | What I did | File(s) | Verified? |
|---|---|---|---|
| W7 | Products: loading skeleton, stale-error clear; Compare selection view + `compare=<id>` preload (see E2) | `pages/Products.jsx`, `pages/Compare.jsx` | CDP |
| W8 | FraudScanner: trim/length validation, clear stale result before rescan | `pages/FraudScanner.jsx` | lint+build |
| W9 | Budget/Goals: in-flight save guard, clear stale error, `deadline: ... || null` | `pages/Budget.jsx`, `pages/Goals.jsx` | CDP create |
| W10 | Simulators (Investments / Investment / WhatIf): clear stale result on re-run; WhatIf amount required for amount scenarios | 3 files | lint+build |
| W11 | DecisionSafety: caution/risky badge classes styled; empty body guard | `pages/DecisionSafety.jsx`, `App.css` | lint+build |
| W12 | IncidentReports: only show "At risk" when >0; option text alignment | `pages/IncidentReports.jsx` | lint+build |
| W13 | Education: quiz button always rendered; empty-answers guard | `pages/Education.jsx` | lint+build |
| W14 | MainLayout + SecurityCenter single dashboard fetch; MainLayout alive guard | 2 files | lint+build |
| W15 | CSS: `.form-warn` (amber partial-data banner); `.risk-badge.level-*` fallbacks incl. `level-caution`/`level-risky`, `level-aggressive`, `level-very-low`, `level-low-moderate`; light-theme chart tooltip/grid in Markets; CountUp respects reduced-motion | `App.css`, `Markets.jsx`, `CountUp.jsx` | lint+build |

## 1.5 Discoveries while fixing
- Live transactions from the API have `riskLevel: null, riskScore: null` (verified via curl on the QA account) → the old "Normal" auto-label was wrong; the new `unanalyzed` (gray 🩶) bucket is required. Same logic added to `RiskAnalysis`.
- `react-hooks/set-state-in-effect` lint (v6) forbids synchronous `setState` inside effect bodies → refactored Alerts/Products/Compare/TransactionDetails/IncidentReports to do state changes only inside promise callbacks; Products merges the `?compare=` pre-select into the fetch `.then`; Compare seeds `loading` from `useState(() => hasSelection)`.
- `ESLint exhaustive-deps` (useMemo `searchParams`) → replaced with `useState` lazy initializer.
- Reports/RiskAnalysis now track per-feed `failedFeeds`; every "none"/"All clear"/"0 open" branch is replaced with an honest "unavailable" note when its feed failed, plus an amber `.form-warn` banner. `.form-warn` CSS added.
- Goals previously relied on Jackson accepting `""` as `LocalDate` (worked in a live probe) → now sends `deadline: null` explicitly.
- Markets detail chart + InvestmentSimulator chart were dark-themed in the light app → tooltips/grid flipped to light (`#ffffff` / `#e6e9f2`).
- Products "Compare with 2–3 products" from a product detail page used a single id (400). Now routes to `/products?compare=<id>` and pre-selects that product in the compare picker.
- Orphan files removed after `rg` confirmed zero imports: `Dashboard.jsx`, `Profile.jsx`, `Spending.jsx`, `ScamScanner.jsx`, `components/RiskBadge.jsx`.

## 2. Verification
- `npm run lint` — PASS (0 errors, 0 warnings) after all batches.
- `npm run build` — PASS (`✓ built`).
- CDP runtime probes (200 responses):
  - **Settings save** → toast now appears ("Financial profile saved."), no "Could not save your profile." error.
  - **AI Assistant** → `GET /assistant/history` network requests observed on `/assistant`.
  - **Markets** → 14 cards render; clicking a card opens the detail panel (no stuck loading); Back to list restores 14 cards.
  - **Products compare** → product detail "Compare with 2–3 products" routes to `/products?compare=1` and pre-selects that product (checked=1).
  - **Goals** → goal created without deadline: row appears, no error.
  - **Budget duplicate** → re-submitting an existing category shows `A budget for "Dining out" already exists. Delete it and re-create to change the limit.` (guard verified against real data).

## 3. Known non-frontend items (not touched, per constraint)
- Unsaved-incident privacy: `GET /api/incidents` returns all users' reports (backend filter needed).
- Forgot-password flow: no backend endpoint exists — frontend placeholder kept as-is (honest UI).

---

# PART II — Backend fix phase (user approved: "FIX BACKEND")

> Same repo, settings/permissions updated for this session: edits authorized for the audited backend items.
> Git: **never** commit/push (explicit user instruction). Verification: `Maven` via direct java classworlds launcher (PowerShell mvn.cmd emits no stdout), tests against local Postgres test DB, live backend restarted from `.env`-loaded config.

### 2026-09-05 — Phase 0 (secrets) + H + M + C fixes
| # | What I did | File(s) | Verified? |
|---|---|---|---|
| C1 | Removed hardcoded Supabase credentials from git-tracked `application.properties` → `${DB_URL}/${DB_USERNAME}/${DB_PASSWORD}`; added `.env` loader (LOWEST_PRECEDENCE, working-dir `.env`, `DB_*` + raw-key aliasing) and registered it via `spring.factories`; `.env` (git-ignored) holds the live values; tightened multipart cap 5 MB/6 MB; added `app.allow-email-free-reset` flag | `application.properties`, new `config/EnvFilePostProcessor.java`, new `META-INF/spring.factories`, `.env` | backend boots from `.env` (Hikari connects, QA login works) |
| C1b | Fail-fast on default dev JWT secret: app now refuses to start without a real `JWT_SECRET` | `config/SecurityStartupValidator.java` | rebuilt app still starts (`.env` supplies secret) |
| C2 | **Cross-user data leak**: `GET /api/incidents` returned every user's scam reports. Scoped to current user via `findByUserIdOrderByCreatedAtDesc` | `controller/IncidentsController.java`, `repository/ScamReportRepository.java` | created QA report → only QA's row appears |
| H4/H3 | JWT lifecycle: `expiresIn` derived from actual token TTL (was hardcoded 86400); password-reset support added | `service/JwtService.java` (+`expirationSeconds()`, purpose-scoped 15-min reset tokens keyed to password hash), `service/AuthService.java`, `controller/AuthController.java`, new `dto/ForgotPasswordRequest/Response`, `dto/ResetPasswordRequest`, `model/User.java` (+`setPasswordHash`) | login shows `expiresIn: 86400`; forgot→reset→relogin round-trip OK; demo flag on, pass-through disabled at 400 for invalid |
| H1 | Dashboard "N dashboard queries": `DashboardService.build` now fetches transactions **once**; analytics computed on the list via new overloads in `FinanceAnalyticsService`; `HealthScoreService.evaluate(User, List<FinancialTransaction>)` added (original signatures preserved for what-if/tests) | `service/DashboardService.java`, `service/FinanceAnalyticsService.java`, `health/HealthScoreService.java` | `/api/dashboard` 200, full shape intact, QA score 82 |
| H2 | Missing transaction isolation on CSV import: `@Transactional` on `TransactionService` (class) + read-only on `list` | `service/TransactionService.java` | tests green |
| M1 | Budgets & Goals only had create/list → added `PUT /{id}` + `DELETE /{id}` with `findByIdAndUserId` ownership check (404 if not yours) | `controller/BudgetController.java`, `controller/GoalController.java`, `model/FinancialGoal.java` (+`update()`), `repository/FinancialGoalRepository.java`, `model/Budget.java` | CRUD round-trip OK; status flips ACTIVE↔COMPLETED |
| M2 | Investment recommendation summary truncated amounts (`setScale(0)`) → now `HALF_UP` | `service/InvestmentRecommendationService.java` | summary shows 101 for ₹100.75 |
| M3 | Simulator years unbounded → `@Max(50)` + defensive clamp | `dto/InvestmentSimulationRequest.java`, `service/InvestmentSimulatorService.java` | years=100 → 400; years=10 → 10 points |
| M5 | Missing FK/query indexes added on hot join columns | entities: `Alert`, `ScamReport`, `FraudAnalysis`, `FraudIndicator`, `FinancialTransaction`, `ChatConversation`, `ChatMessage`, `EducationAttempt`, `QuizQuestion`, `Budget`, `FinancialGoal` | schema recreated in tests (create-drop) + index annotations compile |
| M7 | `FraudIndicator.getAnalysis()` leaked parent data in JSON → `@JsonIgnore` | `model/FraudIndicator.java` | — |
| M9 | Optimistic locking where few writable users exist: `@Version` on Budget / FinancialGoal / FinancialProfile, defined as `bigint default 0` so schema-update backfills existing rows safely | 3 entities | app boots; budget PUT increment works |

### Verification after fix pass
- `clean test` via direct java launcher → **Tests run: 85, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.**
- Test adaptation: `HealthScoreServiceTest` mock stubs moved to the `List` analytics overload (ambiguous after H1); `emptyData` uses `anyList()`.
- Backend restarted from `backend-0.0.1-SNAPSHOT.jar` (old spring-boot:run stopped). Live probes: login, dashboard, budget/goal CRUD, incident scoping, password-reset round-trip, simulator validation, recommendation rounding — all pass.

### Backend items intentionally NOT changed during this pass
- CI blocker: tests use local Postgres `financial_fraud_assistant_test` (create-drop) not Testcontainers.
- Supabase schema: `ddl-auto=update` now adds the new indexes + `version` columns (with default 0) when first run against an existing DB.
- Larger architecture (L1–L6, CSV streaming, external AI calls, rate limiting) and GH Actions remain for a separate pass.

---

# PART III — Database fix phase (user approved: "FIX DATABASE"; scope = the audit's recommended migration order, all fixes)

> Admin-only DB work authorized. Never commit/push (explicit). Verification: Flyway against a lab restore of the live DB first, then against live Supabase (full snapshot backed up before the first live boot). Live state verified via read-only psql through the pooler.

### 2026-09-05 — Flyway adoption + schema standardization + analytics tables
| # | What I did | File(s) | Verified? |
|---|---|---|---|
| D+G | Switched to managed schema evolution: `ddl-auto=validate` in main+prod (test keeps create-drop, flyway disabled there); Flyway enabled with `baseline-on-migrate=true`, `baseline-version=1`, `locations=classpath:db/migration` | `application.properties`, `application-prod.properties`, test `application.properties`, `pom.xml` (flyway-core, flyway-database-postgresql, `flyway.version=10.22.0`) | 86/86 tests, migration-consistency test |
| — | Compose parity: postgres bound to `127.0.0.1:5432`; removed `./database/schema` init mount (Flyway now owns schema); backend service runs `prod` profile | `docker-compose.yml` | — |
| F1/D2 | Baseline schema (V1) with standardized shapes: notes/preferred_categories TEXT, money NUMERIC(12,2), version columns, `uk_budgets_user_category`, CHECKs + composite indexes | `db/migration/V1__baseline.sql` | — |
| D1/D2/D3 | Legacy-align migration (V2): notes→TEXT, preferred→TEXT, 11 money columns→NUMERIC(12,2), enum widths→255, budgets dedupe+unique, `version` columns (`ADD COLUMN IF NOT EXISTS`) | `db/migration/V2__align_legacy_columns.sql` | lab DB converges exactly |
| D4–D8 | Constraints + indexes (V3): 15 guarded CHECKs (risk scores 0–100, goal amounts ≥ε, profile sanity), composite list indexes | `db/migration/V3__constraints_indexes.sql` | lab + live: 20 `chk_*` present |
| F2 | Analytics tables (V4): `assets`, `market_price_history`, `decision_analyses` + FKs/CHECKs/indexes | `db/migration/V4__analytics_tables.sql` | live seeded 14 assets / 742 prices |
| F2-backend | DB-backed markets: `Asset`, `MarketPriceHistory`, `DecisionAnalysis` entities + repos; `MarketService` reads from DB with built-in fallback universe; `DecisionService`/`WhatIfService` persist every analysis; `DataSeeder` seeds assets + 52 weekly prices | `model/*`, `repository/*`, `service/MarketService.java`, `service/DecisionService.java`, `service/WhatIfService.java`, `config/DataSeeder.java` | decision/what-if rows persisted on live |
| — | Entity alignment for validate: notes columnDefinition TEXT, money precision (12,2), preferred TEXT | `model/{FinancialTransaction,FinancialProfile,Budget,FinancialGoal,ScamReport}.java` | Hibernate validate green |
| — | Migration-consistency test (clean+migrate on scratch schema; asserts tables/checks/precision/TEXT/version/indexes) | `test/.../DatabaseMigrationConsistencyTest.java` | 86/86 **BUILD SUCCESS** |
| — | Live anomalies found & fixed during rollout: (1) pooler `statement_timeout` killed the 742-row seed → per-asset chunked inserts + guarded try/catch (seed can never take the app down); (2) live V2 recorded success but most ALTERs never materialized → idempotent guarded convergence migration **V5** (only alters what still mismatches); (3) Hikari 10 starved the 15-slot pooler → prod caps at 8 (min-idle 2, conn timeout 30 s) | `DataSeeder.java`, `db/migration/V5__converge_legacy_columns.sql`, `application-prod.properties` | live boot clean, schema converged |

### Verification after DB-fix pass (live Supabase)
- `clean test` via direct java launcher → **Tests run: 86, Failures: 0, Errors: 0. BUILD SUCCESS.** (85 existing + new migration-consistency test; now 5 migrations.)
- Lab proof first: boot prod profile against a full live snapshot (`ffa_lab`, local PG 17) → Flyway baselined V1, applied V2–V4, Hibernate validate passed, API login OK.
- Live: apps booted prod against Supabase; `flyway_schema_history` = v1..v5 all success; **converged schema** — `notes`/`preferred_categories` TEXT, 11 money columns `NUMERIC(12,2)`, 20 `chk_*` CHECKs, `uk_budgets_user_category`, `version` columns.
- Live data: `assets` 14 + `market_price_history` 742 seeded; `decision_analyses` rows persisted from `/api/decision/analyze` (LOAN, SAFE 91) and `/api/simulator/what-if` (WHAT_IF, SAFE 81); market search + detail DB-backed (RELIANCE: 53 persisted price points, 2025-09-06→2026-09-05, trend Positive, risk MEDIUM).
- Live smoke: login, market search ("HDFC"→HDFCBANK), market detail, decision analyze, what-if — all green; backend left running (prod profile).
- No git changes committed (per instruction); full live snapshot kept at `%TEMP%\opencode\db-backup\fin_shield_pre_fix.sql`.