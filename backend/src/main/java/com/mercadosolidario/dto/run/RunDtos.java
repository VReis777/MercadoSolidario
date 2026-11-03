package com.mercadosolidario.dto.run;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RunDtos {

    public record ModalityInfo(
        String modality,
        String label,
        int slots,
        long registered,
        long remaining
    ) {}

    public record RunInfoResponse(
        BigDecimal amount,
        boolean cardEnabled,
        boolean pixAutomatic,
        List<ModalityInfo> modalities
    ) {}

    public record RunRegisterRequest(
        @NotBlank @Size(max = 255) String fullName,
        @Size(max = 255) String email,
        @Size(max = 60) String phone,
        @NotBlank String modality,
        @Size(max = 10) String shirtSize,
        @NotBlank String paymentMethod,
        BigDecimal extraDonation
    ) {}

    public record RunRegistrationResponse(
        Long id,
        String modality,
        BigDecimal amount,
        BigDecimal extraDonation,
        BigDecimal totalCharged,
        String status,
        String paymentMethod,
        String pixCode,
        String pixQrUri,
        String checkoutUrl,
        LocalDateTime createdAt
    ) {}

    public record RunVerifyResponse(
        Long id,
        String fullName,
        String modality,
        String shirtSize,
        String status,
        LocalDateTime kitDeliveredAt,
        String kitDeliveredBy,
        String qrUri
    ) {}

    public record RunStatusResponse(
        Long id,
        String fullName,
        String modality,
        String shirtSize,
        String status,
        BigDecimal amount,
        LocalDateTime paidAt,
        String checkinCode,
        LocalDateTime kitDeliveredAt,
        String kitDeliveredBy
    ) {}

    public record AdminRunRegistrationResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String modality,
        String shirtSize,
        BigDecimal amount,
        BigDecimal extraDonation,
        BigDecimal totalCharged,
        String paymentMethod,
        String status,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime kitDeliveredAt,
        String kitDeliveredBy
    ) {}
}
