package com.mercadosolidario.dto.history;

import java.math.BigDecimal;
import java.util.List;

public record RegistrationImportPreview(
    String fileName,
    int campaignYear,
    int totalRows,
    int newRows,
    int duplicateInFile,
    int alreadyRegistered,
    int invalidRows,
    int withPreviousHistory,
    List<String> warnings,
    List<Row> rows
) {
    public enum RowStatus { NOVO, DUPLICADO_NO_ARQUIVO, JA_CADASTRADO, INVALIDO }

    public record Row(
        String sheet,
        int rowNumber,
        String name,
        String cpfMasked,
        String phoneMasked,
        String spouseName,
        Integer householdSize,
        BigDecimal totalIncome,
        String referrerName,
        RowStatus status,
        List<String> messages
    ) {}
}
