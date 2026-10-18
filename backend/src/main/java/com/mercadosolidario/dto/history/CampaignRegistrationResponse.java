package com.mercadosolidario.dto.history;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Família cadastrada na campanha (pela data de criação do registro), com cruzamento do histórico. */
public record CampaignRegistrationResponse(
    Long id,
    String responsibleName,
    String cpfMasked,
    Integer householdSize,
    BigDecimal totalIncome,
    String origin,
    String referrerName,
    String status,
    LocalDateTime createdAt,
    HistoryMatch historyMatch,
    List<Integer> historyYears
) {
    /** CPF = mesmo CPF no histórico (confirmado); NOME = mesmo nome sem CPF coincidente (conferir). */
    public enum HistoryMatch { NENHUM, CPF, NOME }
}
