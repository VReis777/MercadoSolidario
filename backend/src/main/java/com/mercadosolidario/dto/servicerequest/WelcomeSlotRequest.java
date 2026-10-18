package com.mercadosolidario.dto.servicerequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record WelcomeSlotRequest(
    @NotNull(message = "A data é obrigatória") LocalDate availableDate,
    @Size(max = 255) String note
) {
}
