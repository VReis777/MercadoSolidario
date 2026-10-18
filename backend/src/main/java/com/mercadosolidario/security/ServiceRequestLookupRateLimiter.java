package com.mercadosolidario.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

@Component
public class ServiceRequestLookupRateLimiter {
    private final int maximumRequests;
    private final long windowMillis;
    private final int maximumTrackedAddresses;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, Window> windows = new HashMap<>();

    public ServiceRequestLookupRateLimiter(
            @Value("${service-requests.lookup.rate-limit:10}") int maximumRequests,
            @Value("${service-requests.lookup.window-seconds:900}") long windowSeconds,
            @Value("${service-requests.lookup.maximum-tracked-addresses:10000}") int maximumTrackedAddresses) {
        if (maximumRequests < 1 || windowSeconds < 1 || windowSeconds > 2_592_000
                || maximumTrackedAddresses < 1 || maximumTrackedAddresses > 100_000) {
            throw new IllegalArgumentException("Os limites de consulta devem ser positivos");
        }
        this.maximumRequests = maximumRequests;
        this.windowMillis = windowSeconds * 1000;
        this.maximumTrackedAddresses = maximumTrackedAddresses;
    }

    public synchronized boolean allow(HttpServletRequest request) {
        long now = clock.millis();
        String address = request.getRemoteAddr();
        if (address == null || address.isBlank()) {
            address = "unknown";
        }

        Window current = windows.get(address);
        if (current != null && now - current.startedAt >= windowMillis) {
            windows.remove(address);
            current = null;
        }
        if (current == null) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowMillis);
            if (windows.size() >= maximumTrackedAddresses) {
                return false;
            }
            current = new Window(now);
            windows.put(address, current);
        }
        if (current.requests >= maximumRequests) {
            return false;
        }
        current.requests++;
        return true;
    }

    private static final class Window {
        private final long startedAt;
        private int requests;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
