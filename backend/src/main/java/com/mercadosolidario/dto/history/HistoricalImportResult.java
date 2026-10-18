package com.mercadosolidario.dto.history;

public record HistoricalImportResult(
    String fileName,
    int campaignYear,
    int imported,
    int skippedDuplicates,
    int skippedAlreadyImported,
    int skippedInvalid
) {}
