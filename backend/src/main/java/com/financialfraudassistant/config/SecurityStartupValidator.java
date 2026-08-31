package com.financialfraudassistant.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(SecurityStartupValidator.class);
    private static final String DEFAULT_DEV_SECRET = "development-only-secret-change-before-deployment-2026";

    private final String jwtSecret;

    public SecurityStartupValidator(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    public void validate() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured and non-empty");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
        if (DEFAULT_DEV_SECRET.equals(jwtSecret)) {
            log.warn("*** WARNING: Using the DEFAULT development JWT_SECRET. This is insecure for production. "
                    + "Set a strong random JWT_SECRET environment variable before deploying. ***");
        }
    }
}
