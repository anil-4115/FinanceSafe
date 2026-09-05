-- Analytics / persistence tables backing features that previously had no data
-- layer: the Markets feature (assets + weekly price history) and the
-- decision-safety / what-if evaluations.

-- ---------------------------------------------------------------------------
-- assets: the investable catalogue that MarketService reads (fallback to a
-- built-in universe when this table is empty)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS assets (
    id SERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    sector VARCHAR(100) NOT NULL,
    base_price NUMERIC(12,2) NOT NULL,
    weekly_volatility NUMERIC(6,4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_assets_symbol UNIQUE (symbol),
    CONSTRAINT chk_assets_base_price CHECK (base_price > 0)
);
CREATE INDEX IF NOT EXISTS idx_assets_symbol ON assets (symbol);

-- ---------------------------------------------------------------------------
-- market_price_history: weekly price points per asset
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS market_price_history (
    id SERIAL PRIMARY KEY,
    asset_id INTEGER NOT NULL,
    price_date DATE NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    CONSTRAINT fk_market_price_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT uk_market_price_asset_date UNIQUE (asset_id, price_date),
    CONSTRAINT chk_market_price_value CHECK (price > 0)
);
CREATE INDEX IF NOT EXISTS idx_market_price_asset ON market_price_history (asset_id);

-- ---------------------------------------------------------------------------
-- decision_analyses: persisted decision-safety and what-if evaluations
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS decision_analyses (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    decision_type VARCHAR(50) NOT NULL,
    amount NUMERIC(12,2),
    input_text TEXT,
    score INTEGER NOT NULL,
    assessment VARCHAR(20) NOT NULL,
    health_before INTEGER,
    health_after INTEGER,
    goal_impact TEXT,
    reasons TEXT,
    recommendations TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_decision_analyses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_decision_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT chk_decision_health_before CHECK (health_before IS NULL OR health_before BETWEEN 0 AND 100),
    CONSTRAINT chk_decision_health_after CHECK (health_after IS NULL OR health_after BETWEEN 0 AND 100)
);
CREATE INDEX IF NOT EXISTS idx_decision_analysis_user ON decision_analyses (user_id);
CREATE INDEX IF NOT EXISTS idx_decision_analysis_created ON decision_analyses (created_at DESC);