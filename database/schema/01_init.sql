CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS financial_profile (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    age_range VARCHAR(50),
    employment_type VARCHAR(100),
    monthly_income NUMERIC(12,2) DEFAULT 0,
    monthly_fixed_expenses NUMERIC(12,2) DEFAULT 0,
    savings NUMERIC(12,2) DEFAULT 0,
    existing_investments NUMERIC(12,2) DEFAULT 0,
    debt NUMERIC(12,2) DEFAULT 0,
    risk_tolerance VARCHAR(50),
    investment_experience VARCHAR(50),
    preferred_categories TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_financial_profile_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS alerts (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    alert_type VARCHAR(50) NOT NULL DEFAULT 'FRAUD_SIGNAL',
    risk_score INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alerts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS scam_reports (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    amount_at_risk NUMERIC(12,2) DEFAULT 0,
    risk_score INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scam_reports_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS financial_transactions (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    transaction_date DATE NOT NULL,
    merchant VARCHAR(255) NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    transaction_type VARCHAR(20) NOT NULL,
    category VARCHAR(100) NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_date
    ON financial_transactions(user_id, transaction_date DESC);

CREATE TABLE IF NOT EXISTS budgets (
    id SERIAL PRIMARY KEY, user_id INT NOT NULL, category VARCHAR(100) NOT NULL,
    monthly_limit NUMERIC(12,2) NOT NULL CHECK (monthly_limit > 0), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_budget_user_category UNIQUE (user_id, category),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS financial_goals (
    id SERIAL PRIMARY KEY, user_id INT NOT NULL, name VARCHAR(255) NOT NULL,
    target_amount NUMERIC(12,2) NOT NULL CHECK (target_amount > 0), current_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    deadline DATE, monthly_contribution NUMERIC(12,2) NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users(id)
);
