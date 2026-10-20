package com.mercadosolidario.controller;

import com.mercadosolidario.model.CashMovement;
import com.mercadosolidario.service.CashMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
public class CashMovementController {

    private final CashMovementService cashMovementService;

    @GetMapping
    public CashMovementService.CashSummary summary() {
        return cashMovementService.summary();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CashMovementService.CashItem create(
            @RequestParam String type,
            @RequestParam BigDecimal amount,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile file) {
        CashMovement saved = cashMovementService.create(type, amount, description, file);
        return new CashMovementService.CashItem(saved.getId(), saved.getType().name(), saved.getAmount(),
            saved.getDescription(), saved.getAttachmentName() != null, saved.getCreatedBy(), saved.getCreatedAt());
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<byte[]> attachment(@PathVariable Long id) {
        CashMovement movement = cashMovementService.attachment(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + movement.getAttachmentName().replace("\"", "") + "\"")
            .contentType(MediaType.parseMediaType(movement.getAttachmentContentType()))
            .body(movement.getAttachmentData());
    }
}
