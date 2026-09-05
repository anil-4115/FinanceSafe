-- FinanceSafe baseline schema (standardized).
-- Applied in full on a fresh database. On an existing database it is treated
-- as the baseline (spring.flyway.baseline-version=1); later ALTER migrations
-- (V2+) bring legacy databases up to this shape.
--
-- Rules followed:
--   * integer identity PKs, all enums as strings, TEXT for long-form content
--   * money columns NUMERIC(12,2)
--   * optimistic-lock `version` columns for JPA @Version entities
--   * indexed foreign keys

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_email_key UNIQUE (email)
);

-- ---------------------------------------------------------------------------
-- financial_profile (one-to-one with users)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS financial_profile (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    age_range VARCHAR(255),
    employment_type VARCHAR(255),
    monthly_income NUMERIC(12,2) DEFAULT 0,
    monthly_fixed_expenses NUMERIC(12,2) DEFAULT 0,
    savings NUMERIC(12,2) DEFAULT 0,
    existing_investments NUMERIC(12,2) DEFAULT 0,
    debt NUMERIC(12,2) DEFAULT 0,
    risk_tolerance VARCHAR(255),
    investment_experience VARCHAR(255),
    preferred_categories TEXT,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_financial_profile_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT financial_profile_user_id_key UNIQUE (user_id),
    CONSTRAINT chk_profile_income CHECK (monthly_income IS NULL OR monthly_income >= 0),
    CONSTRAINT chk_profile_expenses CHECK (monthly_fixed_expenses IS NULL OR monthly_fixed_expenses >= 0),
    CONSTRAINT chk_profile_savings CHECK (savings IS NULL OR savings >= 0),
    CONSTRAINT chk_profile_investments CHECK (existing_investments IS NULL OR existing_investments >= 0),
    CONSTRAINT chk_profile_debt CHECK (debt IS NULL OR debt >= 0)
);

-- ---------------------------------------------------------------------------
-- alerts
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alerts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(255) NOT NULL,
    alert_type VARCHAR(255) NOT NULL DEFAULT 'FRAUD_SIGNAL',
    risk_score INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(255) NOT NULL DEFAULT 'OPEN',
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alerts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_alert_risk_score CHECK (risk_score BETWEEN 0 AND 100)
);

-- ---------------------------------------------------------------------------
-- scam_reports
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scam_reports (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    channel VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    amount_at_risk NUMERIC(12,2) DEFAULT 0,
    risk_score INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scam_reports_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_report_risk_score CHECK (risk_score BETWEEN 0 AND 100),
    CONSTRAINT chk_report_amount_risk CHECK (amount_at_risk IS NULL OR amount_at_risk >= 0)
);

-- ---------------------------------------------------------------------------
-- financial_transactions
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS financial_transactions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    transaction_date DATE NOT NULL,
    merchant VARCHAR(255) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    transaction_type VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    source VARCHAR(255) NOT NULL DEFAULT 'MANUAL',
    notes TEXT,
    risk_score INTEGER,
    risk_level VARCHAR(255),
    risk_reason VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT financial_transactions_amount_check CHECK (amount > 0),
    CONSTRAINT chk_txn_risk_score CHECK (risk_score IS NULL OR risk_score BETWEEN 0 AND 100)
);
CREATE INDEX IF NOT EXISTS idx_txn_user ON financial_transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_date ON financial_transactions (user_id, transaction_date DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_risk_score ON financial_transactions (risk_score);

-- ---------------------------------------------------------------------------
-- budgets
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS budgets (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    category VARCHAR(255) NOT NULL,
    monthly_limit NUMERIC(12,2) NOT NULL,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT budgets_monthly_limit_check CHECK (monthly_limit > 0),
    CONSTRAINT uk_budgets_user_category UNIQUE (user_id, category)
);
CREATE INDEX IF NOT EXISTS idx_budget_user ON budgets (user_id);

-- ---------------------------------------------------------------------------
-- financial_goals
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS financial_goals (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    target_amount NUMERIC(12,2) NOT NULL,
    current_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    deadline DATE,
    monthly_contribution NUMERIC(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT financial_goals_target_amount_check CHECK (target_amount > 0),
    CONSTRAINT chk_goal_current_amount CHECK (current_amount >= 0),
    CONSTRAINT chk_goal_contribution CHECK (monthly_contribution >= 0)
);
CREATE INDEX IF NOT EXISTS idx_goal_user ON financial_goals (user_id);

-- ---------------------------------------------------------------------------
-- chat_conversations / chat_messages
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_conversations (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_conversations_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_chat_conv_user ON chat_conversations (user_id);

CREATE TABLE IF NOT EXISTS chat_messages (
    id SERIAL PRIMARY KEY,
    conversation_id INTEGER NOT NULL,
    sender VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id)
);
CREATE INDEX IF NOT EXISTS idx_chat_msg_conversation ON chat_messages (conversation_id);

-- ---------------------------------------------------------------------------
-- education
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS education_modules (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    duration_mins INTEGER NOT NULL,
    order_index INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS education_attempts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    module_id INTEGER NOT NULL,
    score_pct INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_education_attempts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_education_attempts_module FOREIGN KEY (module_id) REFERENCES education_modules(id),
    CONSTRAINT chk_attempt_score CHECK (score_pct BETWEEN 0 AND 100)
);
CREATE INDEX IF NOT EXISTS idx_edu_attempt_user ON education_attempts (user_id);
CREATE INDEX IF NOT EXISTS idx_edu_attempt_module ON education_attempts (module_id);

CREATE TABLE IF NOT EXISTS quiz_questions (
    id SERIAL PRIMARY KEY,
    module_id INTEGER NOT NULL,
    question TEXT NOT NULL,
    options TEXT NOT NULL,
    correct_index INTEGER NOT NULL,
    explanation TEXT,
    CONSTRAINT fk_quiz_questions_module FOREIGN KEY (module_id) REFERENCES education_modules(id)
);
CREATE INDEX IF NOT EXISTS idx_quiz_module ON quiz_questions (module_id);

-- ---------------------------------------------------------------------------
-- financial_products (reference/catalog data)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS financial_products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    risk_level VARCHAR(255) NOT NULL,
    expected_return VARCHAR(255) NOT NULL,
    liquidity VARCHAR(255) NOT NULL,
    min_amount VARCHAR(255) NOT NULL,
    tenure VARCHAR(255) NOT NULL,
    suitable_for TEXT NOT NULL,
    pros TEXT,
    cons TEXT,
    description TEXT
);

-- ---------------------------------------------------------------------------
-- fraud_analysis / fraud_indicators
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fraud_analyses (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    input_type VARCHAR(255) NOT NULL,
    input VARCHAR(10000) NOT NULL,
    risk_score INTEGER NOT NULL,
    risk_label VARCHAR(255) NOT NULL,
    scam_type VARCHAR(255),
    category VARCHAR(255),
    ai_estimate INTEGER,
    confidence VARCHAR(255) NOT NULL,
    summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fraud_analyses_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_analysis_risk_score CHECK (risk_score BETWEEN 0 AND 100),
    CONSTRAINT chk_ai_estimate CHECK (ai_estimate IS NULL OR ai_estimate BETWEEN -1 AND 100)
);
CREATE INDEX IF NOT EXISTS idx_fraud_analysis_user ON fraud_analyses (user_id);
CREATE INDEX IF NOT EXISTS idx_fraud_analysis_risk ON fraud_analyses (risk_label);
CREATE INDEX IF NOT EXISTS idx_fraud_analysis_user_created ON fraud_analyses (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS fraud_indicators (
    id SERIAL PRIMARY KEY,
    analysis_id INTEGER NOT NULL,
    kind VARCHAR(255) NOT NULL,
    label VARCHAR(2000) NOT NULL,
    weight INTEGER NOT NULL,
    CONSTRAINT fk_fraud_indicator_analysis FOREIGN KEY (analysis_id) REFERENCES fraud_analyses(id),
    CONSTRAINT chk_indicator_weight CHECK (weight BETWEEN 0 AND 100)
);
CREATE INDEX IF NOT EXISTS idx_fraud_indicator_analysis ON fraud_indicators (analysis_id);