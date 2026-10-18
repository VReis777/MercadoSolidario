package com.mercadosolidario.dto.priority;

import java.math.BigDecimal;

public record PriorityFactorResponse(
    String factorCode,
    String label,
    String inputValue,
    BigDecimal contribution,
    boolean matched,
    String explanation
) {}
