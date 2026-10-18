package com.mercadosolidario.dto.distribution;

public record DistributionItemResponse(
    Long id,
    Long productId,
    String productName,
    Integer quantity,
    String unit,
    Long stockMovementId
) {}
