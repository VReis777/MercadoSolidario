package com.mercadosolidario.service;

import com.mercadosolidario.config.DonationProperties;
import com.mercadosolidario.dto.donation.*;
import com.mercadosolidario.dto.stock.StockMovementRequest;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.exception.ResourceNotFoundException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DonationService {

    private static final String PIX_KEY = "PIX_KEY";
    private static final String PIX_MERCHANT_NAME = "PIX_MERCHANT_NAME";
    private static final String PIX_MERCHANT_CITY = "PIX_MERCHANT_CITY";

    private final DonationRepository donationRepository;
    private final ProductRepository productRepository;
    private final SystemSettingRepository settingRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final StockService stockService;
    private final PixPayloadGenerator pixPayloadGenerator;
    private final DonationProperties donationProperties;

    @Transactional
    public PublicDonationResponse createFinancial(CreateFinancialDonationRequest request) {
        validateFinancialAmount(request.getAmount());
        Optional<Donation> duplicate = donationRepository.findByExternalReference(request.getExternalReference());
        if (duplicate.isPresent()) {
            Donation existing = duplicate.get();
            if (existing.getType() != DonationType.FINANCEIRA
                    || existing.getAmount().compareTo(request.getAmount()) != 0
                    || !Boolean.valueOf(request.isAnonymous()).equals(existing.getIsAnonymous())
                    || !sameName(request.isAnonymous() ? null : request.getDonorName(), existing.getDonorName())) {
                throw new ConflictException("A referência da solicitação já foi utilizada com outros dados.");
            }
            return toPublicResponse(existing);
        }

        Donation donation = new Donation();
        donation.setType(DonationType.FINANCEIRA);
        donation.setAmount(request.getAmount());
        donation.setIsAnonymous(request.isAnonymous());
        donation.setDonorName(request.isAnonymous() ? null : trimToNull(request.getDonorName()));
        donation.setStatus(DonationStatus.AGUARDANDO_PAGAMENTO);
        donation.setPaymentMethod("PIX");
        donation.setExternalReference(request.getExternalReference().trim());
        Donation saved = donationRepository.save(donation);
        audit("FINANCIAL_DONATION_PENDING", saved.getId(),
            "type=FINANCEIRA,amount=" + saved.getAmount().toPlainString()
                + ",anonymous=" + Boolean.TRUE.equals(saved.getIsAnonymous()), null);
        return toPublicResponse(saved);
    }

    @Transactional
    public DonationResponse createProductDonation(CreateProductDonationRequest request) {
        Optional<Donation> duplicate = donationRepository.findByExternalReference(request.getExternalReference());
        if (duplicate.isPresent()) {
            Donation existing = duplicate.get();
            if (existing.getType() != DonationType.PRODUTO
                    || existing.getProduct() == null
                    || !existing.getProduct().getId().equals(request.getProductId())
                    || !java.util.Objects.equals(existing.getQuantity(), request.getQuantity())
                    || !Boolean.valueOf(request.isAnonymous()).equals(existing.getIsAnonymous())
                    || !sameName(request.isAnonymous() ? null : request.getDonorName(), existing.getDonorName())) {
                throw new ConflictException("A referência da solicitação já foi utilizada com outros dados.");
            }
            return toResponse(existing);
        }

        Product product = productRepository.findByIdForUpdate(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ConflictException("Produto inativo não pode receber doações.");
        }

        String username = actor();
        LocalDateTime confirmedAt = LocalDateTime.now();
        Donation donation = new Donation();
        donation.setType(DonationType.PRODUTO);
        donation.setProduct(product);
        donation.setQuantity(request.getQuantity());
        donation.setUnit(product.getUnit());
        donation.setIsAnonymous(request.isAnonymous());
        donation.setDonorName(request.isAnonymous() ? null : trimToNull(request.getDonorName()));
        donation.setObservation(trimToNull(request.getObservation()));
        donation.setExternalReference(request.getExternalReference().trim());
        donation.setStatus(DonationStatus.CONFIRMADA_MANUALMENTE);
        donation.setConfirmationDate(confirmedAt);
        donation.setConfirmedBy(username);
        Donation saved = donationRepository.save(donation);

        StockMovementRequest movement = new StockMovementRequest();
        movement.setProductId(product.getId());
        movement.setType(MovementType.ENTRADA_DOACAO);
        movement.setQuantity(request.getQuantity());
        movement.setReason("Doação de produto registrada");
        movement.setObservation("Donation ID " + saved.getId());
        stockService.registerMovement(movement);

        audit("PRODUCT_DONATION_REGISTERED", saved.getId(),
            "type=PRODUTO,productId=" + product.getId()
                + ",quantity=" + request.getQuantity() + ",unit=" + product.getUnit(),
            username);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> list() {
        return donationRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DonationResponse get(Long id) {
        return toResponse(requireDonation(id));
    }

    @Transactional
    public DonationResponse confirmManually(Long id) {
        Donation donation = requireDonationForUpdate(id);
        requirePending(donation);
        String username = actor();
        donation.setStatus(DonationStatus.CONFIRMADA_MANUALMENTE);
        donation.setConfirmationDate(LocalDateTime.now());
        donation.setConfirmedBy(username);
        Donation saved = donationRepository.save(donation);
        audit("DONATION_CONFIRMED_MANUALLY", saved.getId(),
            "type=" + saved.getType() + ",status=" + saved.getStatus(), username);
        return toResponse(saved);
    }

    @Transactional
    public DonationResponse cancel(Long id) {
        return transitionPending(id, DonationStatus.CANCELADA, "DONATION_CANCELLED");
    }

    @Transactional
    public DonationResponse expire(Long id) {
        return transitionPending(id, DonationStatus.EXPIRADA, "DONATION_EXPIRED");
    }

    @Transactional(readOnly = true)
    public PixConfigurationResponse getPixConfiguration() {
        return pixConfiguration();
    }

    @Transactional(readOnly = true)
    public PublicPixInfoResponse getPublicPixInfo() {
        String key = setting(PIX_KEY).orElse("");
        String name = setting(PIX_MERCHANT_NAME).orElse("");
        String city = setting(PIX_MERCHANT_CITY).orElse("");
        boolean configured = !key.isBlank() && !name.isBlank() && !city.isBlank();
        return new PublicPixInfoResponse(configured, configured ? key : null, name, city);
    }

    @Transactional
    public PixConfigurationResponse updatePixConfiguration(PixConfigurationRequest request) {
        String currentKey = setting(PIX_KEY).orElse("");
        String key = request.isClearPixKey()
            ? ""
            : trimToNull(request.getPixKey()) == null ? currentKey : request.getPixKey().trim();
        String currentName = setting(PIX_MERCHANT_NAME).orElse("");
        String currentCity = setting(PIX_MERCHANT_CITY).orElse("");
        String merchantName = trimToNull(request.getMerchantName()) == null
            ? currentName : request.getMerchantName().trim();
        String merchantCity = trimToNull(request.getMerchantCity()) == null
            ? currentCity : request.getMerchantCity().trim();

        if (!key.isBlank() && (merchantName.isBlank() || merchantCity.isBlank())) {
            throw new IllegalArgumentException("Informe nome e cidade do recebedor para habilitar o PIX.");
        }
        if (merchantName.length() > 25 || merchantCity.length() > 15) {
            throw new IllegalArgumentException("Nome do recebedor e cidade excedem os limites do padrão PIX.");
        }

        saveSetting(PIX_KEY, key, "Chave PIX da organização");
        saveSetting(PIX_MERCHANT_NAME, merchantName, "Nome do recebedor PIX");
        saveSetting(PIX_MERCHANT_CITY, merchantCity, "Cidade do recebedor PIX");
        String username = actor();
        auditSettings("PIX_CONFIGURATION_UPDATED",
            "pixKeyConfigured=" + !key.isBlank()
                + ",merchantName=" + merchantName + ",merchantCity=" + merchantCity,
            username);
        return pixConfiguration();
    }

    private DonationResponse transitionPending(Long id, DonationStatus status, String action) {
        Donation donation = requireDonationForUpdate(id);
        requirePending(donation);
        String username = actor();
        donation.setStatus(status);
        Donation saved = donationRepository.save(donation);
        audit(action, saved.getId(), "type=" + saved.getType() + ",status=" + status, username);
        return toResponse(saved);
    }

    private void requirePending(Donation donation) {
        if (donation.getStatus() != DonationStatus.AGUARDANDO_PAGAMENTO) {
            throw new ConflictException("Somente doações aguardando pagamento podem mudar de estado.");
        }
    }

    private Donation requireDonation(Long id) {
        return donationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doação não encontrada"));
    }

    private Donation requireDonationForUpdate(Long id) {
        return donationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doação não encontrada"));
    }

    private void validateFinancialAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("O valor da doação deve ser maior que zero.");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("O valor da doação pode ter no máximo duas casas decimais.");
        }
        if (amount.compareTo(donationProperties.maximumAmount()) > 0) {
            throw new IllegalArgumentException("O valor excede o limite técnico permitido para uma doação.");
        }
    }

    private PublicDonationResponse toPublicResponse(Donation donation) {
        Optional<PixData> pix = pixData();
        if (pix.isEmpty()) {
            return new PublicDonationResponse(
                donation.getId(), donation.getAmount(), donation.getStatus(),
                "PIX_NAO_CONFIGURADO", null, null,
                "Sua doação está aguardando pagamento. A chave PIX ainda não foi configurada pelos responsáveis."
            );
        }
        PixData data = pix.get();
        String payload = pixPayloadGenerator.generate(
            data.key(), data.merchantName(), data.merchantCity(), donation.getAmount());
        String qrCode = pixPayloadGenerator.generateQrCodeDataUri(payload);
        return new PublicDonationResponse(
            donation.getId(), donation.getAmount(), donation.getStatus(),
            "QR_CODE_GERADO_NAO_CONFIRMADO", payload, qrCode,
            "O código PIX foi gerado. Gerar o código não confirma o pagamento; a equipe responsável poderá validar o recebimento."
        );
    }

    private DonationResponse toResponse(Donation donation) {
        Product product = donation.getProduct();
        return new DonationResponse(
            donation.getId(),
            donation.getType(),
            Boolean.TRUE.equals(donation.getIsAnonymous()) ? null : donation.getDonorName(),
            Boolean.TRUE.equals(donation.getIsAnonymous()),
            donation.getAmount(),
            donation.getStatus(),
            product == null ? null : product.getId(),
            product == null ? null : product.getName(),
            donation.getQuantity(),
            donation.getUnit(),
            donation.getObservation(),
            donation.getCreatedAt(),
            donation.getUpdatedAt(),
            donation.getConfirmationDate(),
            donation.getConfirmedBy()
        );
    }

    private PixConfigurationResponse pixConfiguration() {
        String key = setting(PIX_KEY).orElse("");
        String name = setting(PIX_MERCHANT_NAME).orElse("");
        String city = setting(PIX_MERCHANT_CITY).orElse("");
        boolean configured = !key.isBlank() && !name.isBlank() && !city.isBlank();
        String masked = key.isBlank() ? null : "••••" + key.substring(Math.max(0, key.length() - 4));
        return new PixConfigurationResponse(configured, masked, name, city);
    }

    private Optional<PixData> pixData() {
        Optional<String> key = setting(PIX_KEY).filter(value -> !value.isBlank());
        Optional<String> name = setting(PIX_MERCHANT_NAME).filter(value -> !value.isBlank());
        Optional<String> city = setting(PIX_MERCHANT_CITY).filter(value -> !value.isBlank());
        if (key.isEmpty() || name.isEmpty() || city.isEmpty()) return Optional.empty();
        return Optional.of(new PixData(key.get(), name.get(), city.get()));
    }

    private Optional<String> setting(String key) {
        return settingRepository.findByKey(key)
            .map(SystemSetting::getValue)
            .filter(value -> value != null);
    }

    private void saveSetting(String key, String value, String description) {
        SystemSetting setting = settingRepository.findByKey(key).orElseGet(SystemSetting::new);
        setting.setKey(key);
        setting.setValue(value);
        setting.setDescription(description);
        settingRepository.save(setting);
    }

    private void audit(String action, Long entityId, String details, String username) {
        audit(action, "Donation", entityId, details, username);
    }

    private void auditSettings(String action, String details, String username) {
        audit(action, "SystemSetting", null, details, username);
    }

    private void audit(String action, String entityType, Long entityId, String details, String username) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        String actorName = username == null ? currentUsername() : username;
        if (actorName != null) {
            userRepository.findByUsername(actorName).ifPresent(entry::setUser);
        }
        auditLogRepository.save(entry);
    }

    private String actor() {
        String username = currentUsername();
        if (username == null) {
            throw new IllegalStateException("Usuário autenticado obrigatório para esta operação.");
        }
        return username;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }

    private boolean sameName(String requested, String existing) {
        return java.util.Objects.equals(trimToNull(requested), trimToNull(existing));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record PixData(String key, String merchantName, String merchantCity) {}
}
