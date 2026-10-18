package com.mercadosolidario.dto.stock;

public record BasketImportResult(String fileName, int created, int skippedExisting, int skippedDuplicates) {}
