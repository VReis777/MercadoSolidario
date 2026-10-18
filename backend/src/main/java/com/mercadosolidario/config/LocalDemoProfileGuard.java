package com.mercadosolidario.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.Set;

public class LocalDemoProfileGuard implements EnvironmentPostProcessor, Ordered {

    private static final String REQUIRED_DATABASE_URL =
            "jdbc:h2:mem:mercado_solidario_local_demo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String[] profiles = environment.getActiveProfiles();
        if (Arrays.stream(profiles).noneMatch("local-demo"::equals)) {
            return;
        }
        if (profiles.length != 1 || !"true".equals(environment.getProperty("ENABLE_LOCAL_DEMO_AUTH"))) {
            throw new IllegalStateException(
                    "local-demo requires only the local-demo profile and ENABLE_LOCAL_DEMO_AUTH=true.");
        }
        if (Arrays.stream(profiles).anyMatch(profile ->
                Set.of("prod", "production", "staging").contains(profile.toLowerCase()))) {
            throw new IllegalStateException("local-demo cannot run with a production-like profile.");
        }
        if (!"127.0.0.1".equals(environment.getProperty("server.address"))
                || !"8080".equals(environment.getProperty("server.port"))
                || !REQUIRED_DATABASE_URL.equals(environment.getProperty("spring.datasource.url"))
                || !"org.h2.Driver".equals(environment.getProperty("spring.datasource.driver-class-name"))) {
            throw new IllegalStateException(
                    "local-demo requires the fixed loopback server and its dedicated in-memory H2 database.");
        }
        String username = environment.getProperty("LOCAL_DEMO_ADMIN_USERNAME");
        String password = environment.getProperty("LOCAL_DEMO_ADMIN_PASSWORD");
        String fullName = environment.getProperty("LOCAL_DEMO_ADMIN_FULL_NAME");
        if (username == null || username.length() > 100
                || !username.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
                || password == null || password.length() < 8
                || fullName == null || fullName.isBlank() || fullName.length() > 255) {
            throw new IllegalStateException(
                    "local-demo requires valid, terminal-supplied LOCAL_DEMO_ADMIN credentials.");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
