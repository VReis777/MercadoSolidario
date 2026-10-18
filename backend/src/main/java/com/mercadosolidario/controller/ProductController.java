package com.mercadosolidario.controller;

import com.mercadosolidario.dto.stock.BasketImportPreview;
import com.mercadosolidario.dto.stock.BasketImportResult;
import com.mercadosolidario.dto.stock.ProductRequest;
import com.mercadosolidario.dto.stock.ProductResponse;
import com.mercadosolidario.dto.stock.ProductStatusRequest;
import com.mercadosolidario.dto.stock.StockMovementResponse;
import com.mercadosolidario.service.BasketItemImportService;
import com.mercadosolidario.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final StockService stockService;
    private final BasketItemImportService basketItemImportService;

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public BasketImportPreview previewImport(@RequestParam("file") MultipartFile file) {
        return basketItemImportService.preview(file);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public BasketImportResult confirmImport(@RequestParam("file") MultipartFile file) {
        return basketItemImportService.confirm(file);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public List<ProductResponse> list() {
        return stockService.listProducts(true);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public ProductResponse get(@PathVariable Long id) {
        return stockService.getProduct(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return stockService.createProduct(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return stockService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public ProductResponse updateStatus(
            @PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
        return stockService.updateStatus(id, request);
    }

    @GetMapping("/{id}/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public List<StockMovementResponse> movements(@PathVariable Long id) {
        return stockService.productMovements(id);
    }
}
