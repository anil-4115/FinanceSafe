package com.financialfraudassistant.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Order(Ordered.LOWEST_PRECEDENCE)
public class EnvFilePostProcessor implements EnvironmentPostProcessor {

    private static final String ENV_FILE = ".env";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path file = Path.of(ENV_FILE);
        if (!Files.exists(file)) return;

        Map<String, Object> properties = new LinkedHashMap<>();
        try {
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int separator = line.indexOf('=');
                if (separator <= 0) continue;
                String key = line.substring(0, separator).trim();
                if (key.isEmpty()) continue;
                String value = line.substring(separator + 1).trim();
                properties.put(key, value);
                if (key.matches("^[A-Z][A-Z0-9_]*$")) {
                    properties.put(key.toLowerCase(Locale.ROOT).replace('_', '.'), value);
                }
            }
        } catch (IOException ignored) {
            return;
        }

        if (!properties.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource("envFile", properties));
        }
    }
}