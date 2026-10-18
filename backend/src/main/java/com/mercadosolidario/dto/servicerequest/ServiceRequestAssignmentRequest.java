package com.mercadosolidario.dto.servicerequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceRequestAssignmentRequest(
    @NotBlank @Size(max = 100) String username,
    @Size(max = 2000) String observation
) {
}
