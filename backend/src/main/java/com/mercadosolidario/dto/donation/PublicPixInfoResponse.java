package com.mercadosolidario.dto.donation;

public record PublicPixInfoResponse(
        boolean configured,
        String pixKey,
        String merchantName,
        String merchantCity) {
}
