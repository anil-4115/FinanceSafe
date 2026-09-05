package com.financialfraudassistant.service;

import com.financialfraudassistant.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public long expirationSeconds() { return expirationMs / 1000; }

    public String generateResetToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("purpose", "PASSWORD_RESET")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + RESET_TOKEN_TTL_MS))
                .signWith(resetKey(user))
                .compact();
    }

    public boolean isValidResetToken(User user, String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(resetKey(user)).build()
                    .parseSignedClaims(token).getPayload();
            return "PASSWORD_RESET".equals(claims.get("purpose", String.class))
                    && user.getEmail().equals(claims.getSubject());
        } catch (Exception exception) {
            return false;
        }
    }

    private static final long RESET_TOKEN_TTL_MS = 15 * 60 * 1000L;

    private SecretKey resetKey(User user) {
        String material = (user.getPasswordHash() == null ? "" : user.getPasswordHash())
                + "::" + new String(signingKey.getEncoded(), StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(sha256(material));
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    public String extractEmail(String token) { return claims(token).getSubject(); }

    public boolean isValid(String token) {
        try { claims(token); return true; }
        catch (Exception exception) { return false; }
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
