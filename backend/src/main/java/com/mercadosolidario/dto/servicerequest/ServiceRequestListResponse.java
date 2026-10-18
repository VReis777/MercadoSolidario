package com.mercadosolidario.dto.servicerequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mercadosolidario.model.ServiceRequestStatus;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceRequestListResponse(
    Long id,
    ServiceRequestStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String assignedUsername,
    String contact,
    String location,
    Integer householdSize,
    String assistanceType,
    String welcomePreference,
    java.time.LocalDate welcomeDate,
    String welcomeTime
) {
}
