package com.mercadosolidario.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exportação completa do banco (backup JSON) — somente ADMIN.
 * Despeja todas as tabelas públicas via JDBC, sem passar por entidades.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExportController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export() {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables "
                + "WHERE LOWER(table_schema) = 'public' AND table_type = 'BASE TABLE' "
                + "ORDER BY table_name",
            String.class);
        Map<String, Object> dump = new LinkedHashMap<>();
        dump.put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dump.put("app", "mercado-solidario");
        for (String table : tables) {
            dump.put(table, jdbcTemplate.queryForList("SELECT * FROM \"" + table + "\""));
        }
        String filename = "mercado-solidario-backup-"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(dump);
    }
}
