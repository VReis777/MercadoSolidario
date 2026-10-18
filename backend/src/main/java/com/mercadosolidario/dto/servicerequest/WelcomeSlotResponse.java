package com.mercadosolidario.dto.servicerequest;

import java.time.LocalDate;

public record WelcomeSlotResponse(
    Long id,
    LocalDate availableDate,
    String note,
    Boolean active
) {
}
