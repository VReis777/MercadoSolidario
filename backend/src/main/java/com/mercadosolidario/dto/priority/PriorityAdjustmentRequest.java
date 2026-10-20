package com.mercadosolidario.dto.priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PriorityAdjustmentRequest(
    @NotNull(message = "Informe os pontos do ajuste (positivo sobe, negativo desce)")
    BigDecimal adjustment,

    @NotBlank(message = "A justificativa do ajuste é obrigatória")
    @Size(max = 1000, message = "Justificativa deve ter no máximo 1000 caracteres")
    String note
) {}
