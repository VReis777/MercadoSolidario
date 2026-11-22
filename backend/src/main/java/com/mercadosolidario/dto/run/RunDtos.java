package com.mercadosolidario.dto.run;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 60) String phone,
        @NotNull LocalDate birthDate,
        @NotBlank String modality,
        @Size(max = 10) String shirtSize,
        @NotBlank String paymentMethod,
        BigDecimal extraDonation
    ) {}

    public record RunRegistrationResponse(
        Long id,
        String accessKey,
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
        String kitDeliveredBy,
        LocalDate birthDate,
        String checkinCode,
        String paymentRef,
        BigDecimal netAmount
    ) {}

    /** Edição administrativa de dados cadastrais (não mexe em pagamento/status). */
    public record UpdateRunRegistrationRequest(
        String fullName,
        String email,
        String phone,
        String shirtSize,
        String modality
    ) {}

    /** Linha de importação em massa de inscrições (recuperação de banco). */
    public record RunImportRow(
        Long originalId,
        String fullName,
        String email,
        String phone,
        LocalDate birthDate,
        String modality,
        String shirtSize,
        String paymentMethod,
        String status,
        LocalDateTime createdAt
    ) {}

    /** Doação avulsa importada junto com as inscrições. */
    public record RunImportDonation(
        BigDecimal amount,
        String donorName,
        String donorContact,
        String observation
    ) {}

    public record RunImportRequest(
        List<RunImportRow> registrations,
        List<RunImportDonation> donations
    ) {}

    public record RunImportResult(
        int created,
        int skipped,
        List<String> errors
    ) {}

    /** Resumo de pagamento aprovado no Mercado Pago (conferência admin). */
    public record MpPaymentSummary(
        String paymentId,
        String externalReference,
        BigDecimal amount,
        String paymentMethod,
        String payerEmail,
        String payerName,
        String status,
        String dateCreated
    ) {}
}
