package com.mercadosolidario.dto.donation;

import java.math.BigDecimal;

public record DonationGoalItemResponse(
    Long id,
    String name,
    String unit,
    BigDecimal perFamilyQuantity,
    BigDecimal packageSize,
    BigDecimal neededQuantity,
    BigDecimal pledgedQuantity,
    BigDecimal receivedQuantity,
    BigDecimal remainingQuantity,
    long remainingPackages,
    Long productId,
    Integer stockPackages
) {}
