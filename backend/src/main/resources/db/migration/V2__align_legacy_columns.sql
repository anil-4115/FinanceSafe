-- Bring a legacy (hand-written bootstrap + JPA ddl-auto=update) database up to
-- the standardized V1 shape. Every statement is idempotent so this migration is
-- safe on both already-standardized and fresh databases.

-- notes -> TEXT (was VARCHAR(255), truncates CSV imports > 255 chars)
ALTER TABLE financial_transactions ALTER COLUMN notes TYPE TEXT;

-- preferred_categories -> TEXT (was VARCHAR(255))
ALTER TABLE financial_profile ALTER COLUMN preferred_categories TYPE TEXT;

-- money columns -> NUMERIC(12,2) (were unconstrained `numeric`)
ALTER TABLE financial_transactions ALTER COLUMN amount TYPE NUMERIC(12,2) USING amount::NUMERIC(12,2);
ALTER TABLE budgets ALTER COLUMN monthly_limit TYPE NUMERIC(12,2) USING monthly_limit::NUMERIC(12,2);
ALTER TABLE financial_goals ALTER COLUMN target_amount TYPE NUMERIC(12,2) USING target_amount::NUMERIC(12,2);
ALTER TABLE financial_goals ALTER COLUMN current_amount TYPE NUMERIC(12,2) USING current_amount::NUMERIC(12,2);
ALTER TABLE financial_goals ALTER COLUMN monthly_contribution TYPE NUMERIC(12,2) USING monthly_contribution::NUMERIC(12,2);
ALTER TABLE scam_reports ALTER COLUMN amount_at_risk TYPE NUMERIC(12,2) USING amount_at_risk::NUMERIC(12,2);
ALTER TABLE financial_profile ALTER COLUMN monthly_income TYPE NUMERIC(12,2) USING monthly_income::NUMERIC(12,2);
ALTER TABLE financial_profile ALTER COLUMN monthly_fixed_expenses TYPE NUMERIC(12,2) USING monthly_fixed_expenses::NUMERIC(12,2);
ALTER TABLE financial_profile ALTER COLUMN savings TYPE NUMERIC(12,2) USING savings::NUMERIC(12,2);
ALTER TABLE financial_profile ALTER COLUMN existing_investments TYPE NUMERIC(12,2) USING existing_investments::NUMERIC(12,2);
ALTER TABLE financial_profile ALTER COLUMN debt TYPE NUMERIC(12,2) USING debt::NUMERIC(12,2);

-- enum-ish columns widened to 255 (the old bootstrap used VARCHAR(20)/VARCHAR(50))
ALTER TABLE alerts ALTER COLUMN severity TYPE VARCHAR(255);
ALTER TABLE alerts ALTER COLUMN alert_type TYPE VARCHAR(255);
ALTER TABLE alerts ALTER COLUMN status TYPE VARCHAR(255);
ALTER TABLE financial_transactions ALTER COLUMN transaction_type TYPE VARCHAR(255);
ALTER TABLE financial_transactions ALTER COLUMN source TYPE VARCHAR(255);
ALTER TABLE financial_goals ALTER COLUMN status TYPE VARCHAR(255);
ALTER TABLE chat_messages ALTER COLUMN sender TYPE VARCHAR(255);
ALTER TABLE fraud_analyses ALTER COLUMN input_type TYPE VARCHAR(255);

-- budgets: dedupe (keep lowest id per user+category), then enforce the
-- (user_id, category) uniqueness the entity has always declared
DELETE FROM budgets b USING budgets b2
 WHERE b.user_id = b2.user_id AND b.category = b2.category AND b.id > b2.id;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_budgets_user_category') THEN
        ALTER TABLE budgets ADD CONSTRAINT uk_budgets_user_category UNIQUE (user_id, category);
    END IF;
END $$;

-- optimistic-lock `version` columns for the JPA @Version entities
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE financial_goals ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE financial_profile ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;