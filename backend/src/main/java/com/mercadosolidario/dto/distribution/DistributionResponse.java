package com.mercadosolidario.dto.distribution;

import com.mercadosolidario.model.enums.DistributionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record DistributionResponse(
    Long id,
    Long familyId,
    String familyResponsibleName,
    DistributionStatus status,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    String observation,
    String reference,
    String cancellationReason,
    List<DistributionItemResponse> items
) {}
