package com.mercadosolidario.dto.history;

public record RegistrationImportResult(
    String fileName,
    int campaignYear,
    int imported,
    int skippedDuplicates,
    int skippedAlreadyRegistered,
    int skippedInvalid
) {}
