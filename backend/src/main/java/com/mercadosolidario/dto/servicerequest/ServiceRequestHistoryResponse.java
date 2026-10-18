package com.mercadosolidario.dto.servicerequest;

import com.mercadosolidario.model.ServiceRequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceRequestHistoryResponse(
    Long id,
    ServiceRequestStatus previousStatus,
    ServiceRequestStatus newStatus,
    String changedBy,
    LocalDateTime changedAt,
    String observation
) {
}
