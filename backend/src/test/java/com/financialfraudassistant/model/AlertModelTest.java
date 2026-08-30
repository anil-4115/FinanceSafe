package com.financialfraudassistant.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AlertModelTest {

    private final User user = new User("alert@example.com", "hash", "Alert Tester");

    @Test
    void newAlert_isOpen_withDefaultFields() {
        Alert alert = new Alert(user, "High-risk transaction", "Unusual amount detected.",
                Alert.Severity.CRITICAL, "TRANSACTION", 85);

        assertEquals(Alert.Status.OPEN, alert.getStatus());
        assertEquals(Alert.Severity.CRITICAL, alert.getSeverity());
        assertEquals("TRANSACTION", alert.getAlertType());
        assertEquals(Integer.valueOf(85), alert.getRiskScore());
        assertEquals(Alert.Status.OPEN, alert.getStatus());
        assertNotNull(alert.getCreatedAt());
    }

    @Test
    void resolve_marksAlertResolved() {
        Alert alert = new Alert(user, "Scam detected", "KYC scam reported.",
                Alert.Severity.WARNING, "SCAM", 62);

        alert.resolve();

        assertEquals(Alert.Status.RESOLVED, alert.getStatus());
    }

    @Test
    void infoSeverity_roundTrips() {
        Alert alert = new Alert(user, "Budget note", "You used 80% of your food budget.",
                Alert.Severity.INFO, "BUDGET", 30);
        assertEquals(Alert.Severity.INFO, alert.getSeverity());
        assertEquals(Integer.valueOf(30), alert.getRiskScore());
    }
}
