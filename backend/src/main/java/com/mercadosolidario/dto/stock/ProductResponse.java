package com.mercadosolidario.dto.stock;

import java.time.LocalDateTime;

public record ProductResponse(
    Long id,
    String name,
    String category,
    String unit,
    Integer currentQuantity,
    Integer minimumQuantity,
    Integer basketQuantity,
    boolean active,
    boolean lowStock,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String notes
) {}
