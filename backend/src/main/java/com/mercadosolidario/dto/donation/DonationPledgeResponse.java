package com.mercadosolidario.dto.donation;

import com.mercadosolidario.model.enums.DonationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// quantity = número de pacotes prometidos; packageSize/unit = embalagem do item.
public record DonationPledgeResponse(
    Long id,
    Long goalItemId,
    String goalItemName,
    Integer quantity,
    BigDecimal packageSize,
    String unit,
    String donorName,
    String donorContact,
    boolean anonymous,
    boolean selfDelivery,
    DonationStatus status,
    LocalDateTime createdAt,
    LocalDateTime confirmedAt,
    String confirmedBy
) {}
