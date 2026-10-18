package com.mercadosolidario.dto.priority;

import java.math.BigDecimal;

public record PriorityFactorConfigurationResponse(
    String factorCode,
    String label,
    String operator,
    String referenceValue,
    BigDecimal weight,
    boolean enabled,
    String explanation
) {}
