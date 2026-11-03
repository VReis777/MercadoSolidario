package com.mercadosolidario.service;

import com.mercadosolidario.dto.run.RunDtos.*;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.AuditLog;
import com.mercadosolidario.model.Donation;
import com.mercadosolidario.model.RunRegistration;
import com.mercadosolidario.model.SystemSetting;
import com.mercadosolidario.model.enums.DonationStatus;
import com.mercadosolidario.model.enums.DonationType;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.DonationRepository;
import com.mercadosolidario.repository.RunRegistrationRepository;
import com.mercadosolidario.repository.SystemSettingRepository;
import com.mercadosolidario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunService {

    private static final String PRICE_KEY = "RUM_PRICE_CENTS";
    private static final String PIX_KEY = "PIX_KEY";
    private static final String PIX_MERCHANT_NAME = "PIX_MERCHANT_NAME";
    private static final String PIX_MERCHANT_CITY = "PIX_MERCHANT_CITY";
    private static final long DEFAULT_PRICE_CENTS = 7000;
    private static final List<String> MODALITIES = List.of("3KM", "6KM");

    private final RunRegistrationRepository registrationRepository;
    private final DonationRepository donationRepository;
    private final SystemSettingRepository settingRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final InvitationEmailSender emailSender;
    private final RunPaymentGateway paymentGateway;
    private final PixPayloadGenerator pixPayloadGenerator;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional(readOnly = true)
    public RunInfoResponse info() {
        BigDecimal amount = RunPaymentGateway.reais(priceCents());
        List<ModalityInfo> modalities = MODALITIES.stream().map(modality -> {
            int slots = slots(modality);
            long registered = registrationRepository.countActiveByModality(modality);
            String label = "3KM".equals(modality) ? "3 km — caminhada" : "6 km — corrida";
            return new ModalityInfo(modality, label, slots, registered, Math.max(0, slots - registered));
        }).toList();
        return new RunInfoResponse(amount, paymentGateway.isEnabled(), paymentGateway.isEnabled(), modalities);
    }

    @Transactional
    public RunRegistrationResponse register(RunRegisterRequest request) {
        String modality = request.modality().trim().toUpperCase();
        if (!MODALITIES.contains(modality)) {
            throw new IllegalArgumentException("Modalidade inválida — escolha 3KM ou 6KM.");
        }
        String method = request.paymentMethod().trim().toUpperCase();
        if (!method.equals("PIX") && !method.equals("CARTAO")) {
            throw new IllegalArgumentException("Forma de pagamento inválida — PIX ou cartão.");
        }
        if (method.equals("CARTAO") && !paymentGateway.isEnabled()) {
            throw new IllegalArgumentException("Pagamento em cartão estará disponível em breve — use PIX.");
        }

        int slots = slots(modality);
        long registered = registrationRepository.countActiveByModality(modality);
        if (registered >= slots) {
            throw new ConflictException("Vagas esgotadas para " + modality + ".");
        }

        RunRegistration registration = new RunRegistration();
        registration.setFullName(request.fullName().trim());
        registration.setEmail(trimToNull(request.email()));
        registration.setPhone(trimToNull(request.phone()));
        registration.setModality(modality);
        registration.setShirtSize(trimToNull(request.shirtSize()));
        registration.setAmount(RunPaymentGateway.reais(priceCents()));
        if (request.extraDonation() != null) {
            if (request.extraDonation().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Doação extra não pode ser negativa.");
            }
            registration.setExtraDonation(request.extraDonation());
        }
        registration.setPaymentMethod(method);
        registration.setStatus("PENDENTE");
        RunRegistration saved = registrationRepository.save(registration);
        audit("RUN_REGISTRATION_CREATED", saved.getId(),
            "modality=" + modality + ",method=" + method);

        String pixCode = null;
        String pixQrUri = null;
        String checkoutUrl = null;
        if (paymentGateway.isEnabled()) {
            RunPaymentGateway.Charge charge = (method.equals("PIX")
                ? paymentGateway.createPixCharge(saved)
                : paymentGateway.createCardCheckout(saved)).orElse(null);
            if (charge != null) {
                saved.setPaymentRef(charge.paymentRef());
                registrationRepository.save(saved);
                pixCode = charge.pixCode();
                checkoutUrl = charge.checkoutUrl();
            }
        }
        // PIX sem gateway: gera o copia-e-cola com a chave já configurada
        // (valor total = inscrição + doação extra).
        if (method.equals("PIX") && pixCode == null) {
            Optional<PixData> pix = pixData();
            if (pix.isPresent()) {
                pixCode = pixPayloadGenerator.generate(
                    pix.get().key(), pix.get().name(), pix.get().city(), saved.totalCharged());
                pixQrUri = pixPayloadGenerator.generateQrCodeDataUri(pixCode);
            }
        }
        return new RunRegistrationResponse(
            saved.getId(), modality, saved.getAmount(), saved.getExtraDonation(),
            saved.totalCharged(), saved.getStatus(),
            method, pixCode, pixQrUri, checkoutUrl, saved.getCreatedAt());
    }

    private record PixData(String key, String name, String city) {}

    private Optional<PixData> pixData() {
        String key = setting(PIX_KEY);
        String name = setting(PIX_MERCHANT_NAME);
        String city = setting(PIX_MERCHANT_CITY);
        if (key.isBlank() || name.isBlank() || city.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new PixData(key, name, city));
    }

    private String setting(String key) {
        return settingRepository.findByKey(key)
            .map(SystemSetting::getValue)
            .orElse("");
    }

    @Transactional(readOnly = true)
    public RunStatusResponse status(Long id) {
        RunRegistration registration = registrationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        return toStatus(registration);
    }

    /**
     * Chamado pelo webhook do gateway: consulta o status real do pagamento e,
     * se aprovado, confirma a inscrição (idempotente).
     */
    @Transactional
    public void confirmPayment(String paymentId) {
        if (!paymentGateway.isEnabled() || paymentId == null || paymentId.isBlank()) {
            return;
        }
        paymentGateway.approvedExternalReference(paymentId).ifPresent(externalRef -> {
            try {
                Long registrationId = Long.valueOf(externalRef);
                registrationRepository.findByIdForUpdate(registrationId)
                    .filter(r -> "PENDENTE".equals(r.getStatus()))
                    .ifPresent(r -> {
                        r.setPaymentRef(paymentId);
                        markPaid(r);
                    });
            } catch (NumberFormatException ignored) {
                // external_reference inesperada — ignora o webhook.
            }
        });
    }

    /** Confirmação manual (PIX com comprovante) feita por admin enquanto não houver gateway. */
    @Transactional
    public RunStatusResponse markPaidManually(Long id) {
        RunRegistration registration = registrationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if ("PAGA".equals(registration.getStatus())) {
            // Idempotente: não gera doação duplicada na arrecadação.
            return toStatus(registration);
        }
        markPaid(registration);
        return toStatus(registration);
    }

    @Transactional
    public RunStatusResponse cancel(Long id) {
        RunRegistration registration = registrationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if ("PAGA".equals(registration.getStatus())) {
            throw new ConflictException("Inscrição já paga — não pode ser cancelada por aqui.");
        }
        registration.setStatus("CANCELADA");
        audit("RUN_REGISTRATION_CANCELLED", registration.getId(), "modality=" + registration.getModality());
        return toStatus(registrationRepository.save(registration));
    }

    /** Verificação pública pelo QR: dados do inscrito + status + QR imagem. */
    @Transactional(readOnly = true)
    public RunVerifyResponse verify(String code) {
        RunRegistration registration = registrationRepository.findByCheckinCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Código de inscrição inválido."));
        String qr = pixPayloadGenerator.generateQrCodeDataUri(
            frontendBaseUrl + "/rum/verificar/" + code);
        return new RunVerifyResponse(
            registration.getId(), registration.getFullName(), registration.getModality(),
            registration.getShirtSize(), registration.getStatus(),
            registration.getKitDeliveredAt(), registration.getKitDeliveredBy(), qr);
    }

    private String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "sistema";
    }

    /** Marca a retirada do kit (camisa + medalha) — usado pela equipe no local. */
    @Transactional
    public RunVerifyResponse markKitDelivered(String code) {
        RunRegistration registration = registrationRepository.findByCheckinCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Código de inscrição inválido."));
        if (!"PAGA".equals(registration.getStatus())) {
            throw new ConflictException("Inscrição não está paga — kit não liberado.");
        }
        if (registration.getKitDeliveredAt() != null) {
            throw new ConflictException("Kit já retirado em "
                + registration.getKitDeliveredAt() + ".");
        }
        registration.setKitDeliveredAt(LocalDateTime.now());
        registration.setKitDeliveredBy(actor());
        audit("RUN_KIT_DELIVERED", registration.getId(),
            "modality=" + registration.getModality() + ",by=" + actor());
        registrationRepository.save(registration);
        return verify(code);
    }

    @Transactional(readOnly = true)
    public List<AdminRunRegistrationResponse> listAll() {
        return registrationRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(r -> new AdminRunRegistrationResponse(
                r.getId(), r.getFullName(), r.getEmail(), r.getPhone(), r.getModality(),
                r.getShirtSize(), r.getAmount(), r.getExtraDonation(), r.totalCharged(),
                r.getPaymentMethod(), r.getStatus(),
                r.getPaidAt(), r.getCreatedAt(),
                r.getKitDeliveredAt(), r.getKitDeliveredBy()))
            .toList();
    }

    private void markPaid(RunRegistration registration) {
        registration.setStatus("PAGA");
        registration.setPaidAt(LocalDateTime.now());
        if (registration.getCheckinCode() == null) {
            registration.setCheckinCode(UUID.randomUUID().toString().replace("-", ""));
        }
        registrationRepository.save(registration);
        audit("RUN_REGISTRATION_PAID", registration.getId(),
            "modality=" + registration.getModality() + ",method=" + registration.getPaymentMethod()
                + ",ref=" + registration.getPaymentRef());

        // Entra na contagem de arrecadação como doação financeira paga.
        Donation donation = new Donation();
        donation.setType(DonationType.FINANCEIRA);
        donation.setAmount(registration.totalCharged());
        donation.setDonorName(registration.getFullName());
        donation.setDonorContact(registration.getEmail() != null
            ? registration.getEmail() : registration.getPhone());
        donation.setStatus(DonationStatus.PAGA);
        donation.setConfirmationDate(registration.getPaidAt());
        donationRepository.save(donation);

        notifyPaid(registration);
    }

    private void notifyPaid(RunRegistration registration) {
        Set<String> recipients = new LinkedHashSet<>();
        userRepository.findActiveByRoleNames(List.of("ADMIN", "RH_TESOURARIA")).stream()
            .map(u -> u.getEmail())
            .filter(email -> email != null && !email.isBlank())
            .forEach(recipients::add);
        if (fromAddress != null && !fromAddress.isBlank()) {
            recipients.add(fromAddress);
        }
        try {
            if (!recipients.isEmpty()) {
                emailSender.sendToMany(recipients.toArray(String[]::new),
                    "Inscrição paga - Mercado Solidário Rum " + registration.getModality(),
                    """
                    Inscrição confirmada na corrida Mercado Solidário Rum.

                    Nome: %s
                    Modalidade: %s
                    Tamanho da camisa: %s
                    Valor: R$ %s%s
                    Pagamento: %s%s
                    """.formatted(
                        registration.getFullName(),
                        registration.getModality(),
                        registration.getShirtSize() != null ? registration.getShirtSize() : "não informado",
                        registration.getAmount().toPlainString(),
                        registration.getExtraDonation() != null
                            ? " (+ R$ " + registration.getExtraDonation().toPlainString() + " de doação extra)"
                            : "",
                        registration.getPaymentMethod(),
                        registration.getPaymentRef() != null
                            ? " (ref. " + registration.getPaymentRef() + ")" : ""));
            }
            if (registration.getEmail() != null && !registration.getEmail().isBlank()) {
                String verifyUrl = frontendBaseUrl + "/rum/verificar/" + registration.getCheckinCode();
                byte[] qrPng = pixPayloadGenerator.generateQrCodePng(verifyUrl);
                emailSender.sendWithAttachment(registration.getEmail(),
                    "Inscrição confirmada - Mercado Solidário Rum",
                    """
                    Olá, %s!

                    Sua inscrição na corrida Mercado Solidário Rum (%s) está CONFIRMADA.
                    Obrigado por correr com a gente — sua inscrição se transforma em dignidade.

                    Número da inscrição: %d
                    Camisa: %s

                    Seu QR de retirada do kit (camisa + medalha) vai em ANEXO
                    neste e-mail — apresente-o na retirada. Se preferir, abra:
                    %s
                    """.formatted(registration.getFullName(), registration.getModality(),
                        registration.getId(),
                        registration.getShirtSize() != null ? registration.getShirtSize() : "não informado",
                        verifyUrl),
                    "qrcode-inscricao-" + registration.getId() + ".png",
                    qrPng, "image/png");
            }
        } catch (RuntimeException exception) {
            log.warn("Inscrição paga mas e-mail de confirmação não foi enviado.");
        }
    }

    private long priceCents() {
        return settingRepository.findByKey(PRICE_KEY)
            .map(s -> {
                try { return Long.parseLong(s.getValue().trim()); }
                catch (Exception e) { return DEFAULT_PRICE_CENTS; }
            })
            .orElse(DEFAULT_PRICE_CENTS);
    }

    private int slots(String modality) {
        return settingRepository.findByKey("RUM_SLOTS_" + modality)
            .map(s -> {
                try { return Integer.parseInt(s.getValue().trim()); }
                catch (Exception e) { return 100; }
            })
            .orElse(100);
    }

    private RunStatusResponse toStatus(RunRegistration r) {
        return new RunStatusResponse(r.getId(), r.getFullName(), r.getModality(),
            r.getShirtSize(), r.getStatus(), r.getAmount(), r.getPaidAt(),
            r.getCheckinCode(), r.getKitDeliveredAt(), r.getKitDeliveredBy());
    }

    private void audit(String action, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType("RunRegistration");
        entry.setEntityId(entityId);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
