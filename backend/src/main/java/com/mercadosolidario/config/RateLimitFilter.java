package com.mercadosolidario.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite simples em memória por IP: freia força bruta no login e spam
 * nos formulários públicos. Por instância (Render single instance).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    private static final int LOGIN_LIMIT = 10;
    private static final int PUBLIC_POST_LIMIT = 40;
    private static final int MAX_ENTRIES = 20_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private record Window(long windowStart, int count) {}

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Integer limit = limitFor(request);
        if (limit == null || allow(key(request), limit)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
            "{\"message\":\"Muitas tentativas. Aguarde um minuto e tente novamente.\"}");
    }

    private Integer limitFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        if (path.equals("/api/auth/login") || path.equals("/api/auth/set-password")) {
            return LOGIN_LIMIT;
        }
        if (path.startsWith("/api/public/")) {
            return PUBLIC_POST_LIMIT;
        }
        if (path.equals("/api/donations")) {
            return PUBLIC_POST_LIMIT;
        }
        return null;
    }

    private String key(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank()
            ? forwarded.split(",")[0].trim()
            : request.getRemoteAddr();
        return request.getRequestURI() + "|" + ip;
    }

    private boolean allow(String key, int limit) {
        long now = System.currentTimeMillis();
        if (windows.size() > MAX_ENTRIES) {
            windows.clear();
        }
        Window window = windows.compute(key, (k, previous) -> {
            if (previous == null || now - previous.windowStart() > WINDOW_MS) {
                return new Window(now, 1);
            }
            return new Window(previous.windowStart(), previous.count() + 1);
        });
        return window.count() <= limit;
    }
}
