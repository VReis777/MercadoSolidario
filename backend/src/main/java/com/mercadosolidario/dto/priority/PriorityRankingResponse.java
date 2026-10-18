package com.mercadosolidario.dto.priority;

import java.time.LocalDateTime;
import java.util.List;

public record PriorityRankingResponse(
    String status,
    String calculationVersion,
    LocalDateTime calculatedAt,
    List<PriorityCalculationResponse> families
) {}
