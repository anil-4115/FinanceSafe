package com.financialfraudassistant.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private final Map<String, Window> buckets = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;

    public RateLimitFilter(@Value("${app.rate-limit.max-requests:100}") int maxRequests,
                           @Value("${app.rate-limit.window-minutes:1}") long windowMinutes) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMinutes * 60_000L;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (shouldSkip(request)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        long now = System.currentTimeMillis();
        Window window = buckets.compute(key, (k, existing) -> {
            if (existing == null || now - existing.start > windowMillis) {
                return new Window(now, new AtomicInteger(0));
            }
            return existing;
        });

        if (window.count.incrementAndGet() > maxRequests) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Please try again later.\","
                    + "\"errorCode\":\"RATE_LIMITED\",\"status\":429}");
            return;
        }

        chain.doFilter(servletRequest, servletResponse);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/api/health") || !uri.startsWith("/api");
    }

    private static class Window {
        final long start;
        final AtomicInteger count;

        Window(long start, AtomicInteger count) {
            this.start = start;
            this.count = count;
        }
    }
}
