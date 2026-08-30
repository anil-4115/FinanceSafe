# FinanceSafe — Database Design

> Technology: **PostgreSQL 17**, managed by Spring Data JPA / Hibernate.
> Two databases exist: `financial_fraud_assistant` (runtime) and
> `financial_fraud_assistant_test` (integration tests).
> DDL auto-managed via `spring.jpa.hibernate.ddl-auto=update`.

All primary keys are auto-incrementing integers
(`@GeneratedValue(strategy = GenerationType.IDENTITY)`). All enums are stored
as strings (`@Enumerated(EnumType.STRING)`).

---

## 1. Entity-relationship overview

```text
users 1----* financial_transactions
users 1----* financial_goals
users 1----* budgets            (unique: user_id + category)
users 1----* alerts
users 1----* scam_reports
users 1----* fraud_analyses  1---* fraud_indicators
users 1----* chat_conversations 1---* chat_messages
users 1----* education_attempts *----1 education_modules 1---* quiz_questions
users 1----1 financial_profile
financial_products (reference data, no user FK)
education_modules 1---* quiz_questions
```

---

## 2. Schema

### 2.1 `users`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| email | VARCHAR | NOT NULL, **UNIQUE** |
| password_hash | VARCHAR | NOT NULL (BCrypt) |
| full_name | VARCHAR | NOT NULL |
| created_at | TIMESTAMP | NOT NULL (updatable=false) |
| updated_at | TIMESTAMP | nullable |

### 2.2 `financial_transactions`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| user_id | INTEGER | NOT NULL, FK → users |
| transaction_date | DATE | NOT NULL |
| merchant | VARCHAR | NOT NULL |
| amount | NUMERIC(12,2) | NOT NULL |
| transaction_type | VARCHAR | NOT NULL (`INCOME`/`EXPENSE`) |
| category | VARCHAR | NOT NULL |
| source | VARCHAR | NOT NULL (`MANUAL`/`CSV`) |
| notes | VARCHAR | nullable |
| created_at | TIMESTAMP | NOT NULL |
| risk_score | INTEGER | nullable |
| risk_level | VARCHAR | nullable |
| risk_reason | VARCHAR(2000) | nullable |

### 2.3 `financial_goals`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| user_id | INTEGER | NOT NULL, FK → users |
| name | VARCHAR | NOT NULL |
| target_amount | NUMERIC | NOT NULL |
| current_amount | NUMERIC | NOT NULL |
| deadline | DATE | nullable |
| monthly_contribution | NUMERIC | NOT NULL |
| status | VARCHAR | NOT NULL (`ACTIVE`/`COMPLETED`) |
| created_at | TIMESTAMP | NOT NULL |

### 2.4 `budgets`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| user_id | INTEGER | NOT NULL, FK → users |
| category | VARCHAR | NOT NULL |
| monthly_limit | NUMERIC | NOT NULL |

**UNIQUE constraint:** `(user_id, category)`.

### 2.5 `alerts`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| user_id | INTEGER | NOT NULL, FK → users |
| title | VARCHAR | NOT NULL |
| message | TEXT | NOT NULL |
| severity | VARCHAR | NOT NULL (`INFO`/`WARNING`/`CRITICAL`) |
| alert_type | VARCHAR | NOT NULL |
| risk_score | INTEGER | NOT NULL |
| status | VARCHAR | NOT NULL (`OPEN`/`RESOLVED`) |
| resolved_at | TIMESTAMP | nullable |
| created_at | TIMESTAMP | NOT NULL |

### 2.6 `scam_reports`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| user_id | INTEGER | NOT NULL, FK → users |
| channel | VARCHAR | NOT NULL (e.g. `SMS`) |
| description | TEXT | NOT NULL |
| amount_at_risk | NUMERIC | nullable |
| risk_score | INTEGER | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |

### 2.7 `fraud_analyses`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| user_id | INTEGER | NOT NULL, FK → users |
| input_type | VARCHAR | NOT NULL (`TEXT`/`URL`) |
| input | VARCHAR(10000) | NOT NULL |
| risk_score | INTEGER | NOT NULL |
| risk_label | VARCHAR | NOT NULL (`LOW`/`MODERATE`/`HIGH`/`CRITICAL`) |
| scam_type | VARCHAR | nullable |
| category | VARCHAR | nullable |
| ai_estimate | INTEGER | nullable (`-1` = no data) |
| confidence | VARCHAR | NOT NULL |
| summary | TEXT | nullable |
| created_at | TIMESTAMP | NOT NULL |

### 2.8 `fraud_indicators`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| analysis_id | INTEGER | NOT NULL, FK → fraud_analyses |
| kind | VARCHAR | NOT NULL |
| label | VARCHAR(2000) | NOT NULL |
| weight | INTEGER | NOT NULL |

> One analysis → many indicators (the only `@OneToMany` in the model).

### 2.9 `chat_conversations`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| user_id | INTEGER | NOT NULL, FK → users |
| title | VARCHAR | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | nullable |

### 2.10 `chat_messages`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| conversation_id | INTEGER | NOT NULL, FK → chat_conversations |
| sender | VARCHAR | NOT NULL (`USER`/`ASSISTANT`) |
| content | TEXT | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |

### 2.11 `education_modules`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| title | VARCHAR | NOT NULL |
| topic | VARCHAR | NOT NULL |
| category | VARCHAR | NOT NULL |
| content | TEXT | NOT NULL |
| duration_mins | INTEGER | NOT NULL |
| order_index | INTEGER | NOT NULL |

### 2.12 `quiz_questions`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| module_id | INTEGER | NOT NULL, FK → education_modules |
| question | TEXT | NOT NULL |
| options | TEXT | NOT NULL |
| correct_index | INTEGER | NOT NULL |
| explanation | TEXT | nullable |

### 2.13 `education_attempts`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| user_id | INTEGER | NOT NULL, FK → users |
| module_id | INTEGER | NOT NULL, FK → education_modules |
| score_pct | INTEGER | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |

### 2.14 `financial_profile`

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| user_id | INTEGER | NOT NULL, FK → users, **UNIQUE** (one-to-one) |
| age_range | VARCHAR | nullable |
| employment_type | VARCHAR | nullable |
| monthly_income | NUMERIC | nullable |
| monthly_fixed_expenses | NUMERIC | nullable |
| savings | NUMERIC | nullable |
| existing_investments | NUMERIC | nullable |
| debt | NUMERIC | nullable |
| risk_tolerance | VARCHAR | nullable |
| investment_experience | VARCHAR | nullable |
| preferred_categories | VARCHAR | nullable |
| updated_at | TIMESTAMP | nullable |

### 2.15 `financial_products`

Reference/catalogue data (no user FK).

| Column | Type | Constraints |
| ------ | ---- | ----------- |
| id | INTEGER | PK, identity |
| name | VARCHAR | NOT NULL |
| category | VARCHAR | NOT NULL |
| risk_level | VARCHAR | NOT NULL |
| expected_return | VARCHAR | NOT NULL |
| liquidity | VARCHAR | NOT NULL |
| min_amount | VARCHAR | NOT NULL |
| tenure | VARCHAR | NOT NULL |
| suitable_for | TEXT | NOT NULL |
| pros | TEXT | nullable |
| cons | TEXT | nullable |
| description | TEXT | nullable |

---

## 3. Design notes

- **Ownership everywhere:** every user-owned table carries a `user_id` FK.
  Service code always filters by the authenticated user, making the schema
  naturally IDOR-safe at the data layer.
- **Explainability stored:** `fraud_analyses` stores the raw input, risk
  score/label, `ai_estimate`, confidence and summary; `fraud_indicators`
  persists each contributing evidence item and its weight. This is what
  powers the "why?" explanation.
- **TEXT columns** are used for long-form content (descriptions, summaries,
  chat messages, module content, product docs).
- **Reference data** (`financial_products`, `education_modules`,
  `quiz_questions`) is seeded by `DataSeeder` and is independent of users.
