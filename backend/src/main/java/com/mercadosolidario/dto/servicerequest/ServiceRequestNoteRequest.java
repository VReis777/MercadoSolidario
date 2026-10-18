package com.mercadosolidario.dto.servicerequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceRequestNoteRequest(@NotBlank @Size(max = 2000) String observation) {
}
