package com.mercadosolidario.dto.history;

import java.util.List;

public record HistoricalImportPreview(
    String fileName,
    int campaignYear,
    int totalRows,
    int newRows,
    int duplicateInFile,
    int alreadyImported,
    int invalidRows,
    int withoutCpf,
    int alsoInOtherYears,
    int matchingCurrentFamilies,
    List<String> sheetWarnings,
    List<Row> rows
) {
    public enum RowStatus { NOVO, DUPLICADO_NO_ARQUIVO, JA_IMPORTADO, INVALIDO }

    public record Row(
        String sheet,
        int rowNumber,
        String name,
        String cpfMasked,
        String phoneMasked,
        Integer householdSize,
        String origin,
        RowStatus status,
        List<String> messages
    ) {}
}
