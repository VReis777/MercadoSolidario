package com.mercadosolidario.dto.stock;

import com.mercadosolidario.model.enums.MovementType;

import java.time.LocalDateTime;

public record StockMovementResponse(
    Long id,
    Long productId,
    String productName,
    MovementType type,
    Integer quantity,
    Integer previousBalance,
    Integer newBalance,
    String username,
    String reason,
    String observation,
    LocalDateTime createdAt
) {}
