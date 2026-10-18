package com.mercadosolidario.dto.servicerequest;

import com.mercadosolidario.model.ServiceRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceRequestStatusRequest(
    @NotNull ServiceRequestStatus newStatus,
    @Size(max = 2000) String observation
) {
}
