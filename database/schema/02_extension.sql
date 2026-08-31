-- Additional tables to match all JPA entities (8 more tables beyond 01_init.sql)
-- Applied automatically by docker-compose on first database init.

-- Chat assistant
CREATE TABLE IF NOT EXISTS chat_conversations (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_conversations_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id SERIAL PRIMARY KEY,
    conversation_id INT NOT NULL,
    sender VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id)
);

-- Education
CREATE TABLE IF NOT EXISTS education_modules (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    duration_mins INT NOT NULL,
    order_index INT NOT NULL
);

CREATE TABLE IF NOT EXISTS education_attempts (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    module_id INT NOT NULL,
    score_pct INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_education_attempts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_education_attempts_module FOREIGN KEY (module_id) REFERENCES education_modules(id)
);

CREATE TABLE IF NOT EXISTS quiz_questions (
    id SERIAL PRIMARY KEY,
    module_id INT NOT NULL,
    question TEXT NOT NULL,
    options TEXT NOT NULL,
    correct_index INT NOT NULL,
    explanation TEXT,
    CONSTRAINT fk_quiz_questions_module FOREIGN KEY (module_id) REFERENCES education_modules(id)
);

-- Financial products
CREATE TABLE IF NOT EXISTS financial_products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    risk_level VARCHAR(100) NOT NULL,
    expected_return VARCHAR(255) NOT NULL,
    liquidity VARCHAR(255) NOT NULL,
    min_amount VARCHAR(255) NOT NULL,
    tenure VARCHAR(255) NOT NULL,
    suitable_for TEXT NOT NULL,
    pros TEXT,
    cons TEXT,
    description TEXT
);

-- Fraud analysis
CREATE TABLE IF NOT EXISTS fraud_analyses (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    input_type VARCHAR(20) NOT NULL,
    input VARCHAR(10000) NOT NULL,
    risk_score INT NOT NULL,
    risk_label VARCHAR(100) NOT NULL,
    scam_type VARCHAR(255),
    category VARCHAR(255),
    ai_estimate INT,
    confidence VARCHAR(255) NOT NULL,
    summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fraud_analyses_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS fraud_indicators (
    id SERIAL PRIMARY KEY,
    analysis_id INT NOT NULL,
    kind VARCHAR(100) NOT NULL,
    label VARCHAR(2000) NOT NULL,
    weight INT NOT NULL,
    CONSTRAINT fk_fraud_indicator_analysis FOREIGN KEY (analysis_id) REFERENCES fraud_analyses(id)
);

-- New risk columns on financial_transactions (added by JPA ddl-auto=update, applied here for fresh installs)
ALTER TABLE financial_transactions ADD COLUMN IF NOT EXISTS risk_score INT;
ALTER TABLE financial_transactions ADD COLUMN IF NOT EXISTS risk_level VARCHAR(255);
ALTER TABLE financial_transactions ADD COLUMN IF NOT EXISTS risk_reason VARCHAR(2000);
