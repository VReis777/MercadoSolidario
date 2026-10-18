package com.mercadosolidario.dto.publicrequest;

import com.mercadosolidario.model.ServiceRequestStatus;

import java.time.LocalDateTime;

public record ServiceRequestLookupResponse(
    ServiceRequestStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String contactMasked
) {
}
