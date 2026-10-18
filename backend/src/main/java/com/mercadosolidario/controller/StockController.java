package com.mercadosolidario.controller;

import com.mercadosolidario.dto.stock.StockMovementRequest;
import com.mercadosolidario.dto.stock.StockMovementResponse;
import com.mercadosolidario.dto.stock.StockSummaryResponse;
import com.mercadosolidario.dto.stock.ProductResponse;
import com.mercadosolidario.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

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
}
