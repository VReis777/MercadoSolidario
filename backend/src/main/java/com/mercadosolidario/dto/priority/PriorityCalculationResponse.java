package com.mercadosolidario.dto.priority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PriorityCalculationResponse(
    Long familyId,
    BigDecimal score,
    Integer position,
    LocalDateTime calculatedAt,
    String calculationVersion,
    String status,
    List<PriorityFactorResponse> factors,
    boolean demonstration
) {}
