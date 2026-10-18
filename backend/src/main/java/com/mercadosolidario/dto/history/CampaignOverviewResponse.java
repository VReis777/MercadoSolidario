package com.mercadosolidario.dto.history;

import java.util.List;
import java.util.Map;

public record CampaignOverviewResponse(
    int campaignYear,
    Map<Integer, Long> historicalByYear,
    Map<Integer, Long> historicalWithoutCpfByYear,
    long peopleInMoreThanOneYear,
    long registeredThisCampaign,
    long registeredWithHistoryByCpf,
    long registeredWithHistoryByName,
    long peopleInRegisteredFamilies,
    long activeProducts,
    long productsWithoutStock,
    List<String> origins,
    List<String> alerts
) {}
