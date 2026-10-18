package com.mercadosolidario.dto.servicerequest;

import com.mercadosolidario.model.ServiceRequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceRequestResponse(
    Long id,
    ServiceRequestStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String assignedUsername,
    String contact,
    String location,
    Integer householdSize,
    String assistanceType,
    String fullName,
    String contactPreference,
    String message,
    Long assignedUserId,
    String welcomePreference,
    java.time.LocalDate welcomeDate,
    String welcomeTime
) {
}
