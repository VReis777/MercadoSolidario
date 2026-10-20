package com.mercadosolidario.controller;

import com.mercadosolidario.dto.stock.StockMovementRequest;
import com.mercadosolidario.dto.stock.StockMovementResponse;
import com.mercadosolidario.dto.stock.StockSummaryResponse;
import com.mercadosolidario.dto.stock.ProductResponse;
import com.mercadosolidario.service.StockExcelService;
import com.mercadosolidario.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockExcelService stockExcelService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public StockSummaryResponse summary() {
        return stockService.summary();
    }

    @GetMapping("/low")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public List<ProductResponse> lowStock() {
        return stockService.lowStock();
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public List<StockMovementResponse> movements() {
        return stockService.recentMovements();
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public StockMovementResponse registerMovement(@Valid @RequestBody StockMovementRequest request) {
        return stockService.registerMovement(request);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"estoque-mercado-solidario.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(stockExcelService.exportWorkbook());
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public List<StockExcelService.StockImportChange> importPreview(@RequestParam("file") MultipartFile file) {
        return stockExcelService.preview(file);
    }

    @PostMapping(value = "/import/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public StockExcelService.StockImportResult importConfirm(@RequestParam("file") MultipartFile file) {
        return stockExcelService.confirm(file);
    }
}
