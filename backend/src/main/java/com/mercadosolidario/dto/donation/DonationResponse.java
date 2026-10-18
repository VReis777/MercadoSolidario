package com.mercadosolidario.dto.donation;

import com.mercadosolidario.model.enums.DonationStatus;
import com.mercadosolidario.model.enums.DonationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DonationResponse(
    Long id,
    DonationType type,
    String donorName,
    boolean anonymous,
    BigDecimal amount,
    DonationStatus status,
    Long productId,
    String productName,
    Integer quantity,
    String unit,
    String observation,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime confirmedAt,
    String confirmedBy
) {}
