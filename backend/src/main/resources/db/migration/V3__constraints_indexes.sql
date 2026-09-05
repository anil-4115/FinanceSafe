-- Data-integrity CHECK constraints and secondary indexes.
-- Fresh databases already have these via V1; this migration adds the ones that
-- legacy databases are missing. All additions are guarded / idempotent.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_txn_risk_score') THEN
        ALTER TABLE financial_transactions ADD CONSTRAINT chk_txn_risk_score
            CHECK (risk_score IS NULL OR risk_score BETWEEN 0 AND 100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_alert_risk_score') THEN
        ALTER TABLE alerts ADD CONSTRAINT chk_alert_risk_score
            CHECK (risk_score BETWEEN 0 AND 100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_report_risk_score') THEN
        ALTER TABLE scam_reports ADD CONSTRAINT chk_report_risk_score
            CHECK (risk_score BETWEEN 0 AND 100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_report_amount_risk') THEN
        ALTER TABLE scam_reports ADD CONSTRAINT chk_report_amount_risk
            CHECK (amount_at_risk IS NULL OR amount_at_risk >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_analysis_risk_score') THEN
        ALTER TABLE fraud_analyses ADD CONSTRAINT chk_analysis_risk_score
            CHECK (risk_score BETWEEN 0 AND 100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_ai_estimate') THEN
        ALTER TABLE fraud_analyses ADD CONSTRAINT chk_ai_estimate
            CHECK (ai_estimate IS NULL OR ai_estimate BETWEEN -1 AND 100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_indicator_weight') THEN
        ALTER TABLE fraud_indicators ADD CONSTRAINT chk_indicator_weight
            CHECK (weight BETWEEN 0 AND 100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_attempt_score') THEN
        ALTER TABLE education_attempts ADD CONSTRAINT chk_attempt_score
            CHECK (score_pct BETWEEN 0 AND 100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_goal_current_amount') THEN
        ALTER TABLE financial_goals ADD CONSTRAINT chk_goal_current_amount
            CHECK (current_amount >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_goal_contribution') THEN
        ALTER TABLE financial_goals ADD CONSTRAINT chk_goal_contribution
            CHECK (monthly_contribution >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_profile_income') THEN
        ALTER TABLE financial_profile ADD CONSTRAINT chk_profile_income
            CHECK (monthly_income IS NULL OR monthly_income >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_profile_expenses') THEN
        ALTER TABLE financial_profile ADD CONSTRAINT chk_profile_expenses
            CHECK (monthly_fixed_expenses IS NULL OR monthly_fixed_expenses >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_profile_savings') THEN
        ALTER TABLE financial_profile ADD CONSTRAINT chk_profile_savings
            CHECK (savings IS NULL OR savings >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_profile_investments') THEN
        ALTER TABLE financial_profile ADD CONSTRAINT chk_profile_investments
            CHECK (existing_investments IS NULL OR existing_investments >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_profile_debt') THEN
        ALTER TABLE financial_profile ADD CONSTRAINT chk_profile_debt
            CHECK (debt IS NULL OR debt >= 0);
    END IF;
END $$;

-- secondary / composite indexes for the most-used list queries
CREATE INDEX IF NOT EXISTS idx_transactions_risk_score ON financial_transactions (risk_score);
CREATE INDEX IF NOT EXISTS idx_alerts_user_created ON alerts (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reports_user_created ON scam_reports (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_analysis_user_created ON fraud_analyses (user_id, created_at DESC);