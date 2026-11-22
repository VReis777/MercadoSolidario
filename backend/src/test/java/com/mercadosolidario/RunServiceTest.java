package com.mercadosolidario;

import com.mercadosolidario.dto.run.RunDtos.RunRegisterRequest;
import com.mercadosolidario.dto.run.RunDtos.RunStatusResponse;
import com.mercadosolidario.model.Donation;
import com.mercadosolidario.model.RunRegistration;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.DonationRepository;
import com.mercadosolidario.repository.RunRegistrationRepository;
import com.mercadosolidario.repository.SystemSettingRepository;
import com.mercadosolidario.repository.UserRepository;
import com.mercadosolidario.service.DonationService;
import com.mercadosolidario.service.InvitationEmailSender;
import com.mercadosolidario.service.PixPayloadGenerator;
import com.mercadosolidario.service.RunPaymentGateway;
import com.mercadosolidario.service.RunService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunServiceTest {

    @Mock private RunRegistrationRepository registrationRepository;
    @Mock private DonationRepository donationRepository;
    @Mock private SystemSettingRepository settingRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private InvitationEmailSender emailSender;
    @Mock private RunPaymentGateway paymentGateway;
    @Mock private PixPayloadGenerator pixPayloadGenerator;
    @Mock private DonationService donationService;
    @Mock private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private RunService service;

    @BeforeEach
    void setUp() {
        service = new RunService(registrationRepository, donationRepository, settingRepository,
            auditLogRepository, userRepository, emailSender, paymentGateway,
            pixPayloadGenerator, donationService, jdbcTemplate);
    }

    private RunRegistration pending(Long id, String accessKey, String paymentRef) {
        RunRegistration r = new RunRegistration();
        r.setId(id);
        r.setFullName("Teste");
        r.setEmail("teste@example.com");
        r.setPhone("22999990000");
        r.setModality("3KM");
        r.setAmount(new BigDecimal("100.00"));
        r.setPaymentMethod("PIX");
        r.setStatus("PENDENTE");
        r.setAccessKey(accessKey);
        r.setPaymentRef(paymentRef);
        r.setCreatedAt(java.time.LocalDateTime.now());
        return r;
    }

    @Test
    void statusConfirmsPaidWhenGatewayReportsApprovedPaymentRef() {
        RunRegistration r = pending(7L, "key-7", "555");
        when(registrationRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(r));
        when(paymentGateway.isEnabled()).thenReturn(true);
        when(paymentGateway.approvedExternalReference("555")).thenReturn(Optional.of("7"));
        when(userRepository.findActiveByRoleNames(any())).thenReturn(List.of());
        when(pixPayloadGenerator.generateQrCodePng(anyString())).thenReturn(new byte[]{1});
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));

        RunStatusResponse status = service.status(7L, "key-7");

        assertThat(status.status()).isEqualTo("PAGA");
        assertThat(r.getCheckinCode()).isNotNull();
        assertThat(r.getPaidAt()).isNotNull();
        verify(donationRepository).save(any(Donation.class));
    }

    @Test
    void statusConfirmsPaidViaExternalReferenceSearchWhenNoPaymentRef() {
        RunRegistration r = pending(9L, "key-9", null);
        when(registrationRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(r));
        when(paymentGateway.isEnabled()).thenReturn(true);
        when(paymentGateway.hasApprovedPaymentFor("9")).thenReturn(true);
        when(userRepository.findActiveByRoleNames(any())).thenReturn(List.of());
        when(pixPayloadGenerator.generateQrCodePng(anyString())).thenReturn(new byte[]{1});
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));

        RunStatusResponse status = service.status(9L, "key-9");

        assertThat(status.status()).isEqualTo("PAGA");
    }

    @Test
    void statusStaysPendingWhenGatewayHasNoApprovedPayment() {
        RunRegistration r = pending(11L, "key-11", "777");
        when(registrationRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(r));
        when(paymentGateway.isEnabled()).thenReturn(true);
        when(paymentGateway.approvedExternalReference("777")).thenReturn(Optional.empty());
        when(paymentGateway.hasApprovedPaymentFor("11")).thenReturn(false);

        RunStatusResponse status = service.status(11L, "key-11");

        assertThat(status.status()).isEqualTo("PENDENTE");
        verify(donationRepository, never()).save(any());
    }

    @Test
    void statusRejectsWrongAccessKey() {
        RunRegistration r = pending(3L, "right-key", null);
        when(registrationRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.status(3L, "wrong-key"))
            .isInstanceOf(com.mercadosolidario.exception.ResourceNotFoundException.class);
    }

    @Test
    void registerRejectsUnder18() {
        RunRegisterRequest request = new RunRegisterRequest(
            "Menor de Idade", "menor@example.com", "22999990000",
            LocalDate.now().minusYears(10), "3KM", null, "PIX", null);

        assertThatThrownBy(() -> service.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("18 anos");
    }

    @Test
    void registerRejectsMissingPhone() {
        RunRegisterRequest request = new RunRegisterRequest(
            "Sem Telefone", "sem@example.com", "  ",
            LocalDate.now().minusYears(30), "3KM", null, "PIX", null);

        assertThatThrownBy(() -> service.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("telefone");
    }
}
