package com.mercadosolidario.dto.priority;

import java.time.LocalDateTime;
import java.util.List;

public record PriorityConfigurationResponse(
    Long id,
    String code,
    String name,
    int version,
    String explanation,
    boolean active,
    String createdBy,
    String updatedBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<PriorityFactorConfigurationResponse> factors
) {}
