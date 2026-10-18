package com.mercadosolidario.dto.donation;

import com.mercadosolidario.model.enums.DonationStatus;

import java.math.BigDecimal;

public record PublicDonationResponse(
    Long id,
    BigDecimal amount,
    DonationStatus status,
    String pixStatus,
    String pixPayload,
    String qrCodeDataUri,
    String message
) {}
