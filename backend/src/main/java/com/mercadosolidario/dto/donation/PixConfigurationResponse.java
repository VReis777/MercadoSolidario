package com.mercadosolidario.dto.donation;

public record PixConfigurationResponse(
    boolean configured,
    String maskedPixKey,
    String merchantName,
    String merchantCity
) {}
