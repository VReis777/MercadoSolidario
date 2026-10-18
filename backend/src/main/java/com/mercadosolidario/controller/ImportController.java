package com.mercadosolidario.controller;

import com.mercadosolidario.service.PreviousBeneficiaryImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final PreviousBeneficiaryImportService importService;

    @PostMapping("/previous-beneficiaries/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public ResponseEntity<?> importPreviousBeneficiaries(
            @PathVariable String year,
            @RequestParam("file") MultipartFile file) {
        try {
            var result = importService.importFromCsv(file, year);
            return ResponseEntity.ok(Map.of(
                "imported", result.imported(),
                "updated", result.updated(),
                "errors", result.errors(),
                "errorMessages", result.errorMessages()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Erro ao importar arquivo: " + e.getMessage()
            ));
        }
    }
}
