package com.mercadosolidario.dto.history;

public record HistoricalBeneficiaryResponse(
    Long id,
    int campaignYear,
    String name,
    String cpfMasked,
    String phoneMasked,
    Integer householdSize,
    String origin,
    String sourceFile,
    String sourceSheet,
    Integer sourceRow,
    boolean registeredThisCampaign
) {}
