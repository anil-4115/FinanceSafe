package com.financialfraudassistant.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applies the classpath Flyway migrations to a scratch schema on the local test
 * database and asserts the resulting schema is the standardized shape the
 * entities expect. This keeps the bootstrap DDL honest in CI the same way the
 * migration chain keeps the real database honest.
 */
class DatabaseMigrationConsistencyTest {

    private static final String URL = "jdbc:postgresql://localhost:5432/financial_fraud_assistant_test";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String SCHEMA = "migtest";

    @Test
    void migrationsBuildTheStandardizedSchemaOnAFreshDatabase() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .schemas(SCHEMA)
                .cleanDisabled(false)
                .load();

        flyway.clean();
        var result = flyway.migrate();
        try {
            assertEquals(5, result.migrationsExecuted, "expected V1..V5 to have run on a fresh schema");

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                List<String> tables = queryStrings(connection,
                        "SELECT tablename FROM pg_tables WHERE schemaname = ? ORDER BY tablename", SCHEMA);
                for (String expected : List.of("users", "financial_profile", "financial_transactions", "budgets",
                        "financial_goals", "alerts", "scam_reports", "fraud_analyses", "fraud_indicators",
                        "chat_conversations", "chat_messages", "education_modules", "education_attempts",
                        "quiz_questions", "financial_products", "assets", "market_price_history", "decision_analyses")) {
                    assertTrue(tables.contains(expected), "missing table " + expected);
                }

                // budget uniqueness the JPA model always declared
                assertEquals(1, singleInt(connection,
                        "SELECT count(*) FROM pg_constraint WHERE conname = 'uk_budgets_user_category'" +
                                " AND connamespace = (SELECT oid FROM pg_namespace WHERE nspname = ?)", SCHEMA),
                        "budgets must have the (user_id, category) unique constraint");

                // new analytics tables carry their FKs
                assertEquals(1, singleInt(connection,
                        "SELECT count(*) FROM pg_constraint WHERE conname = 'fk_decision_analyses_user'" +
                                " AND connamespace = (SELECT oid FROM pg_namespace WHERE nspname = ?)", SCHEMA));
                assertEquals(1, singleInt(connection,
                        "SELECT count(*) FROM pg_constraint WHERE conname = 'fk_market_price_asset'" +
                                " AND connamespace = (SELECT oid FROM pg_namespace WHERE nspname = ?)", SCHEMA));

                // data-integrity CHECKs are present
                for (String check : List.of("chk_txn_risk_score", "chk_alert_risk_score", "chk_report_risk_score",
                        "chk_analysis_risk_score", "chk_ai_estimate", "chk_indicator_weight", "chk_attempt_score",
                        "chk_goal_current_amount", "chk_goal_contribution", "chk_report_amount_risk",
                        "chk_decision_score", "chk_profile_savings", "chk_profile_debt")) {
                    assertEquals(1, singleInt(connection,
                            "SELECT count(*) FROM pg_constraint WHERE conname = ?" +
                                    " AND connamespace = (SELECT oid FROM pg_namespace WHERE nspname = ?)", check, SCHEMA),
                            "missing check constraint " + check);
                }

                // money columns are standardized to NUMERIC(12,2)
                assertEquals(11, singleInt(connection,
                        "SELECT count(*) FROM information_schema.columns WHERE table_schema = ?" +
                                " AND table_name IN ('financial_transactions','budgets','financial_goals'," +
                                "'financial_profile','scam_reports') AND data_type = 'numeric'" +
                                " AND numeric_precision = 12 AND numeric_scale = 2", SCHEMA),
                        "all money columns must be NUMERIC(12,2)");

                // long-form columns are TEXT again
                assertEquals(1, singleInt(connection,
                        "SELECT count(*) FROM information_schema.columns WHERE table_schema = ?" +
                                " AND table_name = 'financial_transactions' AND column_name = 'notes' AND data_type = 'text'", SCHEMA));
                assertEquals(1, singleInt(connection,
                        "SELECT count(*) FROM information_schema.columns WHERE table_schema = ?" +
                                " AND table_name = 'financial_profile' AND column_name = 'preferred_categories' AND data_type = 'text'", SCHEMA));

                // version columns exist for the optimistic-lock entities
                assertEquals(1, singleInt(connection,
                        "SELECT count(*) FROM information_schema.columns WHERE table_schema = ?" +
                                " AND table_name = 'budgets' AND column_name = 'version'", SCHEMA));

                // composite list indexes are in place
                for (String index : List.of("idx_txn_user", "idx_transactions_user_date", "idx_transactions_risk_score",
                        "idx_alerts_user_created", "idx_reports_user_created", "idx_fraud_analysis_user_created",
                        "idx_decision_analysis_user", "idx_decision_analysis_created", "idx_market_price_asset")) {
                    assertEquals(1, singleInt(connection,
                            "SELECT count(*) FROM pg_indexes WHERE schemaname = ? AND indexname = ?", SCHEMA, index),
                            "missing index " + index);
                }

                // row-level safety: nothing worse than the seed guard placement
                assertFalse(tables.contains("flyway_placeholder"), "sanity check");
            }
        } finally {
            flyway.clean();
        }
    }

    private static List<String> queryStrings(Connection connection, String sql, String... params) throws Exception {
        List<String> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) statement.setString(i + 1, params[i]);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) values.add(rs.getString(1));
            }
        }
        return values;
    }

    private static int singleInt(Connection connection, String sql, String... params) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) statement.setString(i + 1, params[i]);
            try (ResultSet rs = statement.executeQuery()) {
                assertTrue(rs.next(), "expected a result for " + sql);
                return rs.getInt(1);
            }
        }
    }
}