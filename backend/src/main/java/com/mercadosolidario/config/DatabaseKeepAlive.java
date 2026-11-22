package com.mercadosolidario.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mantém o banco ativo com uma consulta trivial diária — evita que o
 * Supabase free tier pause o projeto por inatividade (~7 dias).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseKeepAlive {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelayString = "${app.keepalive.interval-ms:86400000}",
        initialDelayString = "${app.keepalive.initial-delay-ms:300000}")
    public void ping() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (RuntimeException exception) {
            log.warn("Keep-alive do banco falhou: {}", exception.getMessage());
        }
    }
}
