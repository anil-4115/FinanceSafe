package com.financialfraudassistant.service;

import com.financialfraudassistant.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-for-hmac-2026";
    private static final String OTHER_SECRET = "a-completely-different-secret-key-for-ttesting-jwt-2026";

    private final User user = new User("jane@example.com", "hash", "Jane Doe");

    private JwtService service() {
        return new JwtService(SECRET, 86400000L);
    }

    @Test
    void generateToken_isValidAndCarriesEmail() {
        JwtService service = service();
        String token = service.generateToken(user);
        assertTrue(service.isValid(token));
        assertEquals("jane@example.com", service.extractEmail(token));
    }

    @Test
    void isValid_rejectsTamperedToken() {
        String token = service().generateToken(user);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("a") ? "b" : "a");
        assertFalse(service().isValid(tampered));
    }

    @Test
    void isValid_rejectsTokenSignedWithDifferentSecret() {
        String token = service().generateToken(user);
        JwtService otherService = new JwtService(OTHER_SECRET, 86400000L);
        assertFalse(otherService.isValid(token));
    }

    @Test
    void isValid_rejectsGarbageAndNullishToken() {
        assertFalse(service().isValid("not.a.jwt"));
        assertFalse(service().isValid(""));
    }

    @Test
    void isValid_rejectsExpiredToken() {
        JwtService expiredService = new JwtService(SECRET, -1000L);
        String token = expiredService.generateToken(user);
        assertFalse(expiredService.isValid(token));
    }
}