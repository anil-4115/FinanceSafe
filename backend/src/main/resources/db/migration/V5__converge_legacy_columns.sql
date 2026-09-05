-- Convergence pass for legacy databases where an earlier migration recorded
-- success but the connection-pooled live service never materialized every ALTER.
-- Every block is guarded: it only acts when the column/constraint still has the
-- legacy shape, so this migration is a no-op on fresh or already-converged
-- databases and re-runnable in any environment.

-- notes -> TEXT
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_transactions' AND column_name = 'notes'
                 AND data_type <> 'text') THEN
        ALTER TABLE financial_transactions ALTER COLUMN notes TYPE TEXT USING notes::TEXT;
    END IF;
END $$;

-- preferred_categories -> TEXT
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_profile' AND column_name = 'preferred_categories'
                 AND data_type <> 'text') THEN
        ALTER TABLE financial_profile ALTER COLUMN preferred_categories TYPE TEXT USING preferred_categories::TEXT;
    END IF;
END $$;

-- money columns -> NUMERIC(12,2)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_transactions' AND column_name = 'amount'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE financial_transactions ALTER COLUMN amount TYPE NUMERIC(12,2) USING amount::NUMERIC(12,2);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'budgets' AND column_name = 'monthly_limit'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE budgets ALTER COLUMN monthly_limit TYPE NUMERIC(12,2) USING monthly_limit::NUMERIC(12,2);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_goals' AND column_name = 'target_amount'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE financial_goals ALTER COLUMN target_amount TYPE NUMERIC(12,2) USING target_amount::NUMERIC(12,2);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_goals' AND column_name = 'current_amount'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE financial_goals ALTER COLUMN current_amount TYPE NUMERIC(12,2) USING current_amount::NUMERIC(12,2);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_goals' AND column_name = 'monthly_contribution'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE financial_goals ALTER COLUMN monthly_contribution TYPE NUMERIC(12,2) USING monthly_contribution::NUMERIC(12,2);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'scam_reports' AND column_name = 'amount_at_risk'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE scam_reports ALTER COLUMN amount_at_risk TYPE NUMERIC(12,2) USING amount_at_risk::NUMERIC(12,2);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_profile' AND column_name = 'monthly_income'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE financial_profile ALTER COLUMN monthly_income TYPE NUMERIC(12,2) USING monthly_income::NUMERIC(12,2);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_profile' AND column_name = 'monthly_fixed_expenses'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE financial_profile ALTER COLUMN monthly_fixed_expenses TYPE NUMERIC(12,2) USING monthly_fixed_expenses::NUMERIC(12,2);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_profile' AND column_name = 'savings'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE financial_profile ALTER COLUMN savings TYPE NUMERIC(12,2) USING savings::NUMERIC(12,2);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_profile' AND column_name = 'existing_investments'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE financial_profile ALTER COLUMN existing_investments TYPE NUMERIC(12,2) USING existing_investments::NUMERIC(12,2);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_profile' AND column_name = 'debt'
                 AND NOT (data_type = 'numeric' AND numeric_precision = 12 AND numeric_scale = 2)) THEN
        ALTER TABLE financial_profile ALTER COLUMN debt TYPE NUMERIC(12,2) USING debt::NUMERIC(12,2);
    END IF;
END $$;

-- enum-ish columns -> VARCHAR(255)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'alerts' AND column_name = 'severity'
                 AND data_type <> 'character varying' AND character_maximum_length IS DISTINCT FROM 255) THEN
        ALTER TABLE alerts ALTER COLUMN severity TYPE VARCHAR(255);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'alerts' AND column_name = 'alert_type'
                 AND data_type <> 'character varying' AND character_maximum_length IS DISTINCT FROM 255) THEN
        ALTER TABLE alerts ALTER COLUMN alert_type TYPE VARCHAR(255);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'alerts' AND column_name = 'status'
                 AND data_type <> 'character varying' AND character_maximum_length IS DISTINCT FROM 255) THEN
        ALTER TABLE alerts ALTER COLUMN status TYPE VARCHAR(255);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_transactions' AND column_name = 'transaction_type'
                 AND data_type <> 'character varying' AND character_maximum_length IS DISTINCT FROM 255) THEN
        ALTER TABLE financial_transactions ALTER COLUMN transaction_type TYPE VARCHAR(255);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_transactions' AND column_name = 'source'
                 AND data_type <> 'character varying' AND character_maximum_length IS DISTINCT FROM 255) THEN
        ALTER TABLE financial_transactions ALTER COLUMN source TYPE VARCHAR(255);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'financial_goals' AND column_name = 'status'
                 AND data_type <> 'character varying' AND character_maximum_length IS DISTINCT FROM 255) THEN
        ALTER TABLE financial_goals ALTER COLUMN status TYPE VARCHAR(255);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'chat_messages' AND column_name = 'sender'
                 AND data_type <> 'character varying' AND character_maximum_length IS DISTINCT FROM 255) THEN
        ALTER TABLE chat_messages ALTER COLUMN sender TYPE VARCHAR(255);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'fraud_analyses' AND column_name = 'input_type'
                 AND data_type <> 'character varying' AND character_maximum_length IS DISTINCT FROM 255) THEN
        ALTER TABLE fraud_analyses ALTER COLUMN input_type TYPE VARCHAR(255);
    END IF;
END $$;

-- budgets: dedupe + unique (user_id, category)
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