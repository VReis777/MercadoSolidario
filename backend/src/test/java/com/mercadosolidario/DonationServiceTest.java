package com.mercadosolidario;

import com.mercadosolidario.config.DonationProperties;
import com.mercadosolidario.dto.donation.CreateFinancialDonationRequest;
import com.mercadosolidario.dto.donation.CreateProductDonationRequest;
import com.mercadosolidario.dto.donation.PixConfigurationRequest;
import com.mercadosolidario.dto.donation.PublicDonationResponse;
import com.mercadosolidario.dto.stock.StockMovementRequest;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.model.AuditLog;
import com.mercadosolidario.model.Donation;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.model.SystemSetting;
import com.mercadosolidario.model.enums.DonationStatus;
import com.mercadosolidario.model.enums.DonationType;
import com.mercadosolidario.model.enums.MovementType;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.DonationRepository;
import com.mercadosolidario.repository.ProductRepository;
import com.mercadosolidario.repository.SystemSettingRepository;
import com.mercadosolidario.repository.UserRepository;
import com.mercadosolidario.service.DonationService;
import com.mercadosolidario.service.PixPayloadGenerator;
import com.mercadosolidario.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock private DonationRepository donationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SystemSettingRepository settingRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private StockService stockService;

    private DonationService service;

    @BeforeEach
    void setUp() {
        service = new DonationService(
            donationRepository, productRepository, settingRepository, auditLogRepository,
            userRepository, stockService, new PixPayloadGenerator(),
            new DonationProperties(new BigDecimal("100000.00"))
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin-test", "N/A", List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void financialDonationIsPendingAndNoPixIsInventedWhenConfigurationIsMissing() {
        when(donationRepository.findByExternalReference("request-1")).thenReturn(Optional.empty());
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> {
            Donation donation = invocation.getArgument(0);
            donation.setId(4L);
            return donation;
        });
        when(settingRepository.findByKey(anyString())).thenReturn(Optional.empty());
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PublicDonationResponse response = service.createFinancial(financialRequest("10.00", false));

        assertThat(response.status()).isEqualTo(DonationStatus.AGUARDANDO_PAGAMENTO);
        assertThat(response.pixStatus()).isEqualTo("PIX_NAO_CONFIGURADO");
        assertThat(response.pixPayload()).isNull();
        assertThat(response.qrCodeDataUri()).isNull();
        verify(donationRepository).save(argThat(donation ->
            donation.getStatus() == DonationStatus.AGUARDANDO_PAGAMENTO
                && donation.getType() == DonationType.FINANCEIRA
                && donation.getAmount().compareTo(new BigDecimal("10.00")) == 0));
    }

    @Test
    void anonymousDonationDoesNotPersistDonorName() {
        when(donationRepository.findByExternalReference("request-1")).thenReturn(Optional.empty());
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> {
            Donation donation = invocation.getArgument(0);
            donation.setId(5L);
            return donation;
        });
        when(settingRepository.findByKey(anyString())).thenReturn(Optional.empty());
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CreateFinancialDonationRequest request = financialRequest("25.00", true);
        request.setDonorName("Nome não necessário");

        service.createFinancial(request);

        verify(donationRepository).save(argThat(donation ->
            Boolean.TRUE.equals(donation.getIsAnonymous()) && donation.getDonorName() == null));
        verify(auditLogRepository).save(argThat(audit ->
            !audit.getDetails().contains("Nome não necessário")));
    }

    @Test
    void rejectsInvalidFinancialAmounts() {
        CreateFinancialDonationRequest zero = financialRequest("0.00", false);
        CreateFinancialDonationRequest negative = financialRequest("-1.00", false);
        CreateFinancialDonationRequest tooPrecise = financialRequest("1.001", false);
        CreateFinancialDonationRequest excessive = financialRequest("100000.01", false);

        assertThatThrownBy(() -> service.createFinancial(zero)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createFinancial(negative)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createFinancial(tooPrecise)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createFinancial(excessive)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(donationRepository, auditLogRepository);
    }

    @Test
    void configuredPixCreatesAValidLookingPayloadAndQrWithoutConfirmingPayment() {
        when(donationRepository.findByExternalReference("request-1")).thenReturn(Optional.empty());
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> {
            Donation donation = invocation.getArgument(0);
            donation.setId(6L);
            return donation;
        });
        when(settingRepository.findByKey("PIX_KEY")).thenReturn(Optional.of(setting("PIX_KEY", "org@example.org")));
        when(settingRepository.findByKey("PIX_MERCHANT_NAME")).thenReturn(Optional.of(setting("PIX_MERCHANT_NAME", "MERCADO SOLIDARIO")));
        when(settingRepository.findByKey("PIX_MERCHANT_CITY")).thenReturn(Optional.of(setting("PIX_MERCHANT_CITY", "SAO PAULO")));
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PublicDonationResponse response = service.createFinancial(financialRequest("20.00", false));

        assertThat(response.pixStatus()).isEqualTo("QR_CODE_GERADO_NAO_CONFIRMADO");
        assertThat(response.pixPayload()).contains("BR.GOV.BCB.PIX").contains("20.00").endsWith("6304" + response.pixPayload().substring(response.pixPayload().length() - 4));
        assertThat(response.qrCodeDataUri()).startsWith("data:image/svg+xml;base64,");
        assertThat(response.status()).isEqualTo(DonationStatus.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void productDonationDelegatesStockBalanceToOfficialStockServiceExactlyOnce() {
        Product product = new Product();
        product.setId(9L);
        product.setName("Arroz");
        product.setUnit("pacote");
        product.setActive(true);
        when(donationRepository.findByExternalReference("product-request")).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(product));
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> {
            Donation donation = invocation.getArgument(0);
            donation.setId(12L);
            return donation;
        });
        when(userRepository.findByUsername("admin-test")).thenReturn(Optional.empty());
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateProductDonationRequest request = new CreateProductDonationRequest();
        request.setProductId(9L);
        request.setQuantity(3);
        request.setExternalReference("product-request");
        request.setDonorName("Doador");

        var response = service.createProductDonation(request);

        assertThat(response.type()).isEqualTo(DonationType.PRODUTO);
        assertThat(response.status()).isEqualTo(DonationStatus.CONFIRMADA_MANUALMENTE);
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.unit()).isEqualTo("pacote");
        verify(stockService, times(1)).registerMovement(argThat((StockMovementRequest movement) ->
            movement.getProductId().equals(9L)
                && movement.getQuantity() == 3
                && movement.getType() == MovementType.ENTRADA_DOACAO));
    }

    @Test
    void repeatedProductRequestWithSameReferenceDoesNotIncreaseStockTwice() {
        Product product = new Product();
        product.setId(9L);
        product.setName("Arroz");
        product.setUnit("pacote");
        Donation existing = new Donation();
        existing.setId(12L);
        existing.setType(DonationType.PRODUTO);
        existing.setProduct(product);
        existing.setQuantity(3);
        existing.setUnit("pacote");
        existing.setIsAnonymous(false);
        existing.setDonorName("Doador");
        existing.setStatus(DonationStatus.CONFIRMADA_MANUALMENTE);
        when(donationRepository.findByExternalReference("product-request")).thenReturn(Optional.of(existing));
        CreateProductDonationRequest request = new CreateProductDonationRequest();
        request.setProductId(9L);
        request.setQuantity(3);
        request.setExternalReference("product-request");
        request.setDonorName("Doador");

        service.createProductDonation(request);

        verify(stockService, never()).registerMovement(any());
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void reusingAnIdempotencyReferenceForDifferentFinancialInputConflicts() {
        Donation existing = new Donation();
        existing.setType(DonationType.FINANCEIRA);
        existing.setAmount(new BigDecimal("10.00"));
        existing.setIsAnonymous(false);
        existing.setDonorName("Doador A");
        when(donationRepository.findByExternalReference("request-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createFinancial(financialRequest("20.00", false)))
            .isInstanceOf(ConflictException.class);
        verify(donationRepository, never()).save(any());
    }

    @Test
    void cancelAndExpireOnlyChangePendingDonations() {
        Donation pendingForCancel = pendingDonation(21L);
        Donation pendingForExpire = pendingDonation(22L);
        when(donationRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(pendingForCancel));
        when(donationRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(pendingForExpire));
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername("admin-test")).thenReturn(Optional.empty());
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.cancel(21L).status()).isEqualTo(DonationStatus.CANCELADA);
        assertThat(service.expire(22L).status()).isEqualTo(DonationStatus.EXPIRADA);
        verify(auditLogRepository, times(2)).save(any());
    }

    @Test
    void paidOrAlreadyProcessedDonationCannotBeTransitioned() {
        Donation completed = pendingDonation(23L);
        completed.setStatus(DonationStatus.CONFIRMADA_MANUALMENTE);
        when(donationRepository.findByIdForUpdate(23L)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service.cancel(23L)).isInstanceOf(ConflictException.class);
        verify(donationRepository, never()).save(any());
    }

    @Test
    void pixConfigurationAuditAndReadResponseDoNotExposeTheFullKey() {
        when(settingRepository.findByKey(anyString())).thenReturn(Optional.empty());
        when(settingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername("admin-test")).thenReturn(Optional.empty());
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PixConfigurationRequest request = new PixConfigurationRequest();
        request.setPixKey("chave-oficial-segreda");
        request.setMerchantName("MERCADO SOLIDARIO");
        request.setMerchantCity("SAO PAULO");

        var response = service.updatePixConfiguration(request);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(response.maskedPixKey()).isNull();
        assertThat(auditCaptor.getValue().getEntityType()).isEqualTo("SystemSetting");
        assertThat(auditCaptor.getValue().getDetails()).doesNotContain("chave-oficial-segreda");
        ArgumentCaptor<com.mercadosolidario.model.SystemSetting> settingCaptor =
            ArgumentCaptor.forClass(com.mercadosolidario.model.SystemSetting.class);
        verify(settingRepository, times(3)).save(settingCaptor.capture());
        assertThat(settingCaptor.getAllValues())
            .anySatisfy(setting -> assertThat(setting.getValue()).isEqualTo("chave-oficial-segreda"));
    }

    private CreateFinancialDonationRequest financialRequest(String amount, boolean anonymous) {
        CreateFinancialDonationRequest request = new CreateFinancialDonationRequest();
        request.setAmount(new BigDecimal(amount));
        request.setDonorName("Doador");
        request.setAnonymous(anonymous);
        request.setExternalReference("request-1");
        return request;
    }

    private SystemSetting setting(String key, String value) {
        SystemSetting setting = new SystemSetting();
        setting.setKey(key);
        setting.setValue(value);
        return setting;
    }

    private Donation pendingDonation(Long id) {
        Donation donation = new Donation();
        donation.setId(id);
        donation.setType(DonationType.FINANCEIRA);
        donation.setAmount(new BigDecimal("10.00"));
        donation.setStatus(DonationStatus.AGUARDANDO_PAGAMENTO);
        donation.setIsAnonymous(true);
        return donation;
    }
}
