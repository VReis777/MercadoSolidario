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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private static final List<String> MODALITIES = List.of("3KM", "7KM");

    private final RunRegistrationRepository registrationRepository;
    private final DonationRepository donationRepository;
    private final SystemSettingRepository settingRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final InvitationEmailSender emailSender;
    private final RunPaymentGateway paymentGateway;
    private final PixPayloadGenerator pixPayloadGenerator;
    private final DonationService donationService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.mail.notify:${app.mail.from:}}")
    private String notifyAddress;

    @Transactional(readOnly = true)
    public RunInfoResponse info() {
        BigDecimal amount = RunPaymentGateway.reais(priceCents());
        List<ModalityInfo> modalities = MODALITIES.stream().map(modality -> {
            int slots = slots(modality);
            long registered = registrationRepository.countActiveByModality(modality);
            String label = "3KM".equals(modality) ? "3 km — caminhada" : "7 km — corrida";
            return new ModalityInfo(modality, label, slots, registered, Math.max(0, slots - registered));
        }).toList();
        return new RunInfoResponse(amount, paymentGateway.isEnabled(), paymentGateway.isEnabled(), modalities);
    }

    @Transactional
    public RunRegistrationResponse register(RunRegisterRequest request) {
        String modality = request.modality().trim().toUpperCase();
        if (!MODALITIES.contains(modality)) {
            throw new IllegalArgumentException("Modalidade inválida — escolha 3KM ou 7KM.");
        }
        String method = request.paymentMethod().trim().toUpperCase();
        if (!method.equals("PIX") && !method.equals("CARTAO")) {
            throw new IllegalArgumentException("Forma de pagamento inválida — PIX ou cartão.");
        }

        if (trimToNull(request.phone()) == null) {
            throw new IllegalArgumentException("Informe telefone/WhatsApp — é por ele que enviaremos os avisos da retirada do kit.");
        }
        LocalDate birthDate = request.birthDate();
        LocalDate today = LocalDate.now();
        if (birthDate == null || birthDate.isAfter(today.minusYears(18)) || birthDate.isAfter(today)) {
            throw new IllegalArgumentException("A corrida e a caminhada são exclusivas para maiores de 18 anos.");
        }

        // Uma pessoa = uma inscrição ativa: mesmo e-mail ou mesmo telefone
        // (comparando só os dígitos) bloqueia duplicidade e vaga presa.
        String emailNorm = trimToNull(request.email());
        String phoneDigits = digits(trimToNull(request.phone()));
        boolean duplicate = registrationRepository.findByStatusNot("CANCELADA").stream()
            .anyMatch(r -> (emailNorm != null && emailNorm.equalsIgnoreCase(trimToNull(r.getEmail())))
                || (phoneDigits != null && phoneDigits.equals(digits(trimToNull(r.getPhone())))));
        if (duplicate) {
            throw new ConflictException(
                "Já existe uma inscrição ativa com este e-mail ou telefone. "
                    + "Abra o link que enviamos por e-mail ou fale com a equipe — não é preciso se inscrever de novo.");
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
        registration.setBirthDate(birthDate);
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
        registration.setAccessKey(UUID.randomUUID().toString().replace("-", ""));
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
            saved.getId(), saved.getAccessKey(), modality, saved.getAmount(), saved.getExtraDonation(),
            saved.amountDue(), saved.getStatus(),
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

    @Transactional
    public RunStatusResponse status(Long id, String key) {
        RunRegistration registration = registrationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        // Sem a chave correta a inscrição "não existe": impede enumeração por id.
        if (key == null || registration.getAccessKey() == null
                || !registration.getAccessKey().equals(key)) {
            throw new ResourceNotFoundException("Inscrição não encontrada");
        }
        // Confirmação sob demanda: se o webhook do gateway não chegou,
        // consultamos o Mercado Pago agora — a tela de retorno faz polling.
        if ("PENDENTE".equals(registration.getStatus()) && paymentGateway.isEnabled()) {
            boolean approved = registration.getPaymentRef() != null
                && paymentGateway.approvedExternalReference(registration.getPaymentRef())
                    .map(externalRef -> registration.getId().toString().equals(externalRef))
                    .orElse(false);
            if (!approved) {
                approved = paymentGateway.hasApprovedPaymentFor(registration.getId().toString());
            }
            if (approved) {
                confirmViaGateway(registration,
                    () -> paymentGateway.approvedNetReceivedForReference(registration.getId().toString()));
            }
        }
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
            // Doações avulsas usam external_reference "DON-<id>".
            if (externalRef != null && externalRef.startsWith("DON-")) {
                try {
                    donationService.confirmCardPayment(Long.valueOf(externalRef.substring(4)));
                } catch (NumberFormatException ignored) {
                    // referência inesperada — ignora.
                }
                return;
            }
            try {
                Long registrationId = Long.valueOf(externalRef);
                registrationRepository.findByIdForUpdate(registrationId)
                    .filter(r -> "PENDENTE".equals(r.getStatus()))
                    .ifPresent(r -> {
                        r.setPaymentRef(paymentId);
                        confirmViaGateway(r,
                            () -> paymentGateway.approvedNetReceived(paymentId));
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
        // PIX manual/comprovante: sem taxa de gateway — líquido = bruto.
        markPaid(registration, registration.totalCharged());
        return toStatus(registration);
    }

    /**
     * Sincroniza com o Mercado Pago: se já existe pagamento aprovado com a
     * referência da inscrição, confirma na hora (cobre webhook atrasado/ausente).
     * Fallback: pagamentos antigos podem ter external_reference de um ID
     * anterior à migração de banco — nesse caso casa pelo e-mail do pagador.
     */
    @Transactional
    public AdminRunRegistrationResponse syncPayment(Long id) {
        RunRegistration registration = registrationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if ("PENDENTE".equals(registration.getStatus()) && paymentGateway.isEnabled()) {
            if (paymentGateway.hasApprovedPaymentFor(registration.getId().toString())) {
                confirmViaGateway(registration,
                    () -> paymentGateway.approvedNetReceivedForReference(registration.getId().toString()));
            } else if (paymentGateway instanceof MercadoPagoGateway mp) {
                mp.approvedPaymentForPayerEmail(registration.getEmail()).ifPresent(match -> {
                    // Segurança: se a referência do pagamento aponta para uma
                    // inscrição ativa existente, o dinheiro é dela — não confirmar aqui.
                    Long refId = parseLong(match.externalReference());
                    boolean belongsToOther = refId != null
                        && registrationRepository.findById(refId)
                            .map(r -> !"CANCELADA".equals(r.getStatus()))
                            .orElse(false);
                    if (belongsToOther) {
                        audit("RUN_SYNC_PAYMENT_SKIPPED", registration.getId(),
                            "paymentRef=" + match.paymentId() + " pertence à inscrição #" + refId);
                        return;
                    }
                    registration.setPaymentRef(match.paymentId());
                    if (match.paymentMethod() != null && !match.paymentMethod().isBlank()) {
                        registration.setPaymentMethod(match.paymentMethod());
                    }
                    audit("RUN_PAYMENT_SYNCED_BY_PAYER", registration.getId(),
                        "paymentRef=" + match.paymentId() + ",externalRef=" + match.externalReference());
                    markPaid(registration,
                        match.netAmount() != null ? match.netAmount() : estimateNet(registration));
                });
            }
        }
        return toAdmin(registrationRepository.save(registration));
    }

    private static Long parseLong(String value) {
        try {
            return value == null ? null : Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Últimos pagamentos aprovados no Mercado Pago (conferência admin). */
    @Transactional(readOnly = true)
    public List<MpPaymentSummary> listMpPayments() {
        if (paymentGateway instanceof MercadoPagoGateway mp) {
            return mp.recentApprovedPayments();
        }
        return List.of();
    }

    /**
     * Importação em massa de inscrições e doações avulsas — usada para
     * reconstruir o banco após migração. Aceita ID original explícito
     * (preserva external_reference do Mercado Pago) quando livre.
     * Registros PAGA geram doação e disparam os e-mails de QR.
     */
    @Transactional
    public RunImportResult importRegistrations(RunImportRequest request) {
        List<String> errors = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        if (request.registrations() != null) {
            for (RunImportRow row : request.registrations()) {
                try {
                    if (importOne(row)) {
                        created++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    errors.add((row.fullName() != null ? row.fullName() : "?") + ": " + e.getMessage());
                }
            }
        }
        if (request.donations() != null) {
            for (RunImportDonation d : request.donations()) {
                try {
                    if (d.amount() == null || d.amount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("valor inválido");
                    }
                    Donation donation = new Donation();
                    donation.setType(DonationType.FINANCEIRA);
                    donation.setAmount(d.amount());
                    donation.setDonorName(trimToNull(d.donorName()));
                    donation.setDonorContact(trimToNull(d.donorContact()));
                    donation.setObservation(trimToNull(d.observation()));
                    donation.setStatus(DonationStatus.PAGA);
                    donation.setConfirmationDate(LocalDateTime.now());
                    donationRepository.save(donation);
                    created++;
                } catch (Exception e) {
                    errors.add("Doação " + (d.donorName() != null ? d.donorName() : "?") + ": " + e.getMessage());
                }
            }
        }
        // Garante que a sequence não colida com IDs explícitos importados.
        jdbcTemplate.execute(
            "SELECT setval(pg_get_serial_sequence('run_registrations','id'), "
                + "COALESCE((SELECT MAX(id) FROM run_registrations), 1))");
        audit("RUN_IMPORT", null,
            "created=" + created + ",skipped=" + skipped + ",errors=" + errors.size());
        return new RunImportResult(created, skipped, errors);
    }

    private boolean importOne(RunImportRow row) {
        String name = trimToNull(row.fullName());
        if (name == null) {
            throw new IllegalArgumentException("nome obrigatório");
        }
        String modality = row.modality() != null ? row.modality().trim().toUpperCase() : "";
        if (!MODALITIES.contains(modality)) {
            throw new IllegalArgumentException("modalidade inválida (use 3KM ou 7KM)");
        }
        String status = row.status() != null ? row.status().trim().toUpperCase() : "PENDENTE";
        if (!status.equals("PENDENTE") && !status.equals("PAGA")) {
            throw new IllegalArgumentException("status inválido (use PENDENTE ou PAGA)");
        }
        String method = row.paymentMethod() != null && !row.paymentMethod().isBlank()
            ? row.paymentMethod().trim().toUpperCase() : "PIX";
        if (!method.equals("PIX") && !method.equals("CARTAO")) {
            throw new IllegalArgumentException("método inválido (PIX ou CARTAO)");
        }
        String emailNorm = trimToNull(row.email());
        String phoneDigits = digits(trimToNull(row.phone()));
        boolean duplicate = registrationRepository.findByStatusNot("CANCELADA").stream()
            .anyMatch(r -> (emailNorm != null && emailNorm.equalsIgnoreCase(trimToNull(r.getEmail())))
                || (phoneDigits != null && phoneDigits.equals(digits(trimToNull(r.getPhone())))));
        if (duplicate) {
            return false;
        }
        Long explicitId = row.originalId();
        if (explicitId != null && registrationRepository.existsById(explicitId)) {
            throw new IllegalArgumentException("ID " + explicitId + " já existe");
        }
        Long newId = explicitId != null ? explicitId
            : jdbcTemplate.queryForObject("SELECT nextval('run_registrations_id_seq')", Long.class);
        boolean paid = status.equals("PAGA");
        String checkin = paid ? UUID.randomUUID().toString().replace("-", "") : null;
        jdbcTemplate.update(
            "INSERT INTO run_registrations (id, full_name, email, phone, birth_date, modality,"
                + " shirt_size, amount, payment_method, status, paid_at, net_amount,"
                + " checkin_code, access_key, legacy, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            newId, name, emailNorm, trimToNull(row.phone()), row.birthDate(), modality,
            trimToNull(row.shirtSize()), RunPaymentGateway.reais(priceCents()), method, status,
            paid ? LocalDateTime.now() : null, null, checkin,
            UUID.randomUUID().toString().replace("-", ""), paid, row.createdAt() != null ? row.createdAt() : LocalDateTime.now());
        if (paid) {
            RunRegistration saved = registrationRepository.findById(newId).orElseThrow();
            markPaid(saved, estimateNet(saved));
        }
        return true;
    }

    /**
     * Vincula à inscrição pendente o pagamento aprovado que ficou preso a
     * OUTRA inscrição (ex.: a pessoa pagou pelo link/código de outra).
     * A inscrição de origem precisa estar estornada/cancelada antes.
     */
    @Transactional
    public AdminRunRegistrationResponse claimPayment(Long id, Long fromId) {
        RunRegistration target = registrationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if (!"PENDENTE".equals(target.getStatus())) {
            throw new ConflictException("Só é possível vincular pagamento a inscrição pendente.");
        }
        RunRegistration source = registrationRepository.findByIdForUpdate(fromId)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição de origem não encontrada"));
        if ("PAGA".equals(source.getStatus())) {
            throw new ConflictException("Estorne o pagamento da inscrição de origem antes de vincular.");
        }
        if (!paymentGateway.isEnabled()) {
            throw new ConflictException("Gateway de pagamento indisponível no momento.");
        }
        RunPaymentGateway.ApprovedPaymentInfo info = paymentGateway
            .approvedPaymentInfoForReference(source.getId().toString())
            .orElseThrow(() -> new ConflictException(
                "Nenhum pagamento aprovado encontrado no Mercado Pago para a inscrição #" + fromId + "."));
        target.setPaymentRef(info.paymentId());
        if (info.paymentMethod() != null && !info.paymentMethod().isBlank()) {
            target.setPaymentMethod(info.paymentMethod());
        }
        source.setPaymentRef(null);
        registrationRepository.save(source);
        audit("RUN_PAYMENT_CLAIMED", target.getId(),
            "from=" + fromId + ",paymentRef=" + info.paymentId()
                + ",method=" + target.getPaymentMethod());
        markPaid(target, info.netAmount() != null ? info.netAmount() : estimateNet(target));
        return toAdmin(registrationRepository.save(target));
    }

    /**
     * Cria uma nova inscrição PENDENTE copiando os dados de outra —
     * usado quando a inscrição original precisa ser excluída (ex.:
     * pagamento atribuído à pessoa errada).
     */
    @Transactional
    public AdminRunRegistrationResponse recadastrar(Long id) {
        RunRegistration source = registrationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        RunRegistration copy = new RunRegistration();
        copy.setFullName(source.getFullName());
        copy.setEmail(source.getEmail());
        copy.setPhone(source.getPhone());
        copy.setBirthDate(source.getBirthDate());
        copy.setModality(source.getModality());
        copy.setShirtSize(source.getShirtSize());
        copy.setAmount(source.getAmount());
        copy.setExtraDonation(source.getExtraDonation());
        copy.setPaymentMethod(source.getPaymentMethod());
        copy.setStatus("PENDENTE");
        copy.setAccessKey(UUID.randomUUID().toString().replace("-", ""));
        RunRegistration saved = registrationRepository.save(copy);
        audit("RUN_REGISTRATION_RECREATED", saved.getId(),
            "from=" + id + ",name=" + saved.getFullName());
        return toAdmin(saved);
    }

    /** Edição de dados cadastrais pela equipe (nome, contato, tamanho da camisa). */
    @Transactional
    public AdminRunRegistrationResponse update(Long id, UpdateRunRegistrationRequest request) {
        RunRegistration registration = registrationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        registration.setFullName(request.fullName().trim());
        registration.setEmail(trimToNull(request.email()));
        registration.setPhone(trimToNull(request.phone()));
        registration.setShirtSize(trimToNull(request.shirtSize()));
        if (request.modality() != null && !request.modality().isBlank()) {
            String modality = request.modality().trim().toUpperCase();
            if (!MODALITIES.contains(modality)) {
                throw new IllegalArgumentException("Modalidade inválida — escolha 3KM ou 7KM.");
            }
            registration.setModality(modality);
        }
        audit("RUN_REGISTRATION_UPDATED", registration.getId(),
            "name=" + registration.getFullName() + ",shirt=" + registration.getShirtSize());
        return toAdmin(registrationRepository.save(registration));
    }

    /** Envia e-mail de retentativa para inscrito pendente (instabilidade no pagamento). */
    @Transactional(readOnly = true)
    public void sendRetryEmail(Long id) {
        RunRegistration registration = registrationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if (registration.getEmail() == null || registration.getEmail().isBlank()) {
            throw new ConflictException("Inscrição sem e-mail — avise pelo WhatsApp.");
        }
        String statusUrl = frontendBaseUrl + "/run/inscricao/" + registration.getId()
            + "?key=" + registration.getAccessKey();
        emailSender.sendToMany(new String[]{registration.getEmail()},
            "Sua inscrição no Mercado Solidário Run — pagamento",
            """
            Olá, %s!

            Sentimos muito pela instabilidade no pagamento da sua inscrição
            no Mercado Solidário Run (%s). O problema já foi corrigido.

            PRIMEIRO, abra este link da sua inscrição:

            %s

            - Se você JÁ pagou: a confirmação acontece automaticamente
              nesta página e o QR code do kit chega aqui no seu e-mail.
            - Se ainda NÃO pagou: use o botão "Pagar agora" aqui nesta
              mesma página — sua inscrição continua valendo, não precisa
              se inscrever de novo (uma pessoa = uma inscrição).

            Sua vaga está garantida. Obrigado por correr com a gente!
            Equipe Mercado Solidário
            """.formatted(registration.getFullName(), registration.getModality(), statusUrl));
        audit("RUN_RETRY_EMAIL_SENT", registration.getId(), "to=" + registration.getEmail());
    }

    /**
     * Gera link de checkout do gateway para inscrição pendente (ex.: cartão
     * que estava indisponível na inscrição). Retorna a URL e também envia
     * por e-mail ao inscrito quando houver e-mail cadastrado.
     */
    @Transactional
    public String paymentLink(Long id) {
        RunRegistration registration = registrationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if (!"PENDENTE".equals(registration.getStatus())) {
            throw new ConflictException("Só é possível gerar link para inscrição pendente.");
        }
        if (!paymentGateway.isEnabled()) {
            throw new ConflictException("Gateway de pagamento indisponível no momento.");
        }
        RunPaymentGateway.Charge charge = paymentGateway.createCardCheckout(registration)
            .orElseThrow(() -> new ConflictException("Não foi possível gerar o link de pagamento — tente novamente."));
        registration.setPaymentRef(charge.paymentRef());
        registrationRepository.save(registration);
        audit("RUN_PAYMENT_LINK_CREATED", registration.getId(),
            "method=" + registration.getPaymentMethod() + ",ref=" + charge.paymentRef());

        if (registration.getEmail() != null && !registration.getEmail().isBlank()) {
            try {
                emailSender.sendToMany(new String[]{registration.getEmail()},
                    "Link de pagamento — Mercado Solidário Run",
                    """
                    Olá, %s!

                    Aqui está o link para concluir o pagamento da sua inscrição
                    no Mercado Solidário Run (%s — R$ %s):

                    %s

                    Assim que o pagamento for aprovado, o QR code do seu kit
                    chega automaticamente neste e-mail.

                    Obrigado por correr com a gente!
                    Equipe Mercado Solidário
                    """.formatted(registration.getFullName(), registration.getModality(),
                        registration.amountDue().toPlainString(), charge.checkoutUrl()));
            } catch (RuntimeException exception) {
                log.warn("Link de pagamento gerado mas e-mail não enviado.");
            }
        }
        return charge.checkoutUrl();
    }

    /**
     * Link de pagamento self-service: a pessoa abre o link do e-mail de
     * retentativa, cai na tela de status e pode gerar uma nova cobrança
     * (a chave de acesso da inscrição funciona como credencial).
     */
    @Transactional
    public RunPaymentGateway.Charge publicPaymentLink(Long id, String key) {
        RunRegistration registration = registrationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if (key == null || registration.getAccessKey() == null
                || !registration.getAccessKey().equals(key)) {
            throw new ResourceNotFoundException("Inscrição não encontrada");
        }
        if (!"PENDENTE".equals(registration.getStatus())) {
            throw new ConflictException("Esta inscrição não está pendente de pagamento.");
        }
        if (!paymentGateway.isEnabled()) {
            throw new ConflictException("Pagamento online indisponível no momento — a equipe enviará as instruções pelo WhatsApp/e-mail.");
        }
        RunPaymentGateway.Charge charge = "PIX".equals(registration.getPaymentMethod())
            ? paymentGateway.createPixCharge(registration).orElse(null)
            : paymentGateway.createCardCheckout(registration).orElse(null);
        if (charge == null) {
            throw new ConflictException("Não foi possível gerar o link de pagamento — tente novamente em instantes.");
        }
        registration.setPaymentRef(charge.paymentRef());
        registrationRepository.save(registration);
        audit("RUN_PUBLIC_PAYMENT_LINK", registration.getId(),
            "method=" + registration.getPaymentMethod());
        return charge;
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

    /**
     * Estorna uma confirmação: volta a inscrição para PENDENTE e remove a
     * doação gerada na confirmação — usado quando o pagamento na verdade
     * pertence a outra inscrição (ex.: PIX copia-e-cola trocado).
     */
    @Transactional
    public RunStatusResponse revertPayment(Long id) {
        RunRegistration registration = registrationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if (!"PAGA".equals(registration.getStatus())) {
            throw new ConflictException("Só é possível estornar inscrição paga.");
        }
        // Remove a doação criada na confirmação (mesmo doador, valor e data).
        donationRepository.findAll().stream()
            .filter(d -> d.getType() == DonationType.FINANCEIRA
                && d.getStatus() == DonationStatus.PAGA
                && registration.getFullName().equalsIgnoreCase(d.getDonorName())
                && registration.totalCharged().compareTo(d.getAmount()) == 0
                && registration.getPaidAt() != null
                && d.getConfirmationDate() != null
                && java.time.Duration.between(
                    d.getConfirmationDate(), registration.getPaidAt()).abs()
                    .compareTo(java.time.Duration.ofMinutes(10)) < 0)
            .findFirst()
            .ifPresent(donationRepository::delete);
        registration.setStatus("PENDENTE");
        registration.setPaidAt(null);
        audit("RUN_REGISTRATION_PAYMENT_REVERTED", registration.getId(),
            "modality=" + registration.getModality() + ",name=" + registration.getFullName());
        return toStatus(registrationRepository.save(registration));
    }

    /** Exclui inscrição não paga (PENDENTE ou CANCELADA) — ex.: testes e tentativas duplicadas. */
    @Transactional
    public void delete(Long id) {
        RunRegistration registration = registrationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if ("PAGA".equals(registration.getStatus())) {
            throw new ConflictException("Inscrição paga não pode ser excluída — tem registro financeiro vinculado.");
        }
        audit("RUN_REGISTRATION_DELETED", registration.getId(),
            "modality=" + registration.getModality() + ",name=" + registration.getFullName());
        registrationRepository.delete(registration);
    }

    /** Verificação pública pelo QR: dados do inscrito + status + QR imagem. */
    @Transactional(readOnly = true)
    public RunVerifyResponse verify(String code) {
        RunRegistration registration = registrationRepository.findByCheckinCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Código de inscrição inválido."));
        String qr = pixPayloadGenerator.generateQrCodeDataUri(
            frontendBaseUrl + "/run/verificar/" + code);
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
            .map(this::toAdmin)
            .toList();
    }

    private AdminRunRegistrationResponse toAdmin(RunRegistration r) {
        return new AdminRunRegistrationResponse(
            r.getId(), r.getFullName(), r.getEmail(), r.getPhone(), r.getModality(),
            r.getShirtSize(), r.getAmount(), r.getExtraDonation(), r.amountDue(),
            r.getPaymentMethod(), r.getStatus(),
            r.getPaidAt(), r.getCreatedAt(),
            r.getKitDeliveredAt(), r.getKitDeliveredBy(), r.getBirthDate(), r.getCheckinCode(),
            r.getPaymentRef(), r.getNetAmount());
    }

    /**
     * Confirma via gateway detectando a forma REAL de pagamento no MP
     * (cobre quem trocou cartão↔PIX dentro do checkout) e o líquido
     * exato; cai na estimativa de taxa quando o valor não vem.
     */
    private void confirmViaGateway(RunRegistration registration,
                                   java.util.function.Supplier<java.util.Optional<BigDecimal>> netLookup) {
        paymentGateway.approvedPaymentMethodForReference(registration.getId().toString())
            .ifPresent(method -> {
                if (!method.equals(registration.getPaymentMethod())) {
                    audit("RUN_PAYMENT_METHOD_CORRECTED", registration.getId(),
                        "de=" + registration.getPaymentMethod() + ",para=" + method);
                    registration.setPaymentMethod(method);
                }
            });
        markPaid(registration, netLookup.get().orElseGet(() -> estimateNet(registration)));
    }

    private void markPaid(RunRegistration registration, BigDecimal netAmount) {
        registration.setStatus("PAGA");
        registration.setPaidAt(LocalDateTime.now());
        registration.setNetAmount(netAmount);
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
                    "Inscrição paga - Mercado Solidário Run " + registration.getModality(),
                    """
                    Inscrição confirmada na corrida Mercado Solidário Run.

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
        } catch (RuntimeException exception) {
            log.warn("Inscrição paga mas e-mail à equipe não foi enviado.");
        }
        try {
            sendQrEmail(registration);
        } catch (RuntimeException exception) {
            log.warn("Inscrição paga mas e-mail de confirmação não foi enviado.");
        }
    }

    /** Reenvia o e-mail com o QR de retirada — usado pela equipe quando o primeiro envio falhou. */
    @Transactional
    public void resendQrEmail(Long id) {
        RunRegistration registration = registrationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if (!"PAGA".equals(registration.getStatus())) {
            throw new ConflictException("Só é possível reenviar o QR de inscrição paga.");
        }
        sendQrEmail(registration);
        audit("RUN_QR_EMAIL_RESENT", registration.getId(), "to=" + registration.getEmail());
    }

    private void sendQrEmail(RunRegistration registration) {
        if (registration.getEmail() == null || registration.getEmail().isBlank()) {
            log.warn("Inscrição {} sem e-mail — QR não enviado.", registration.getId());
            return;
        }
        String verifyUrl = frontendBaseUrl + "/run/verificar/" + registration.getCheckinCode();
        byte[] qrPng = pixPayloadGenerator.generateQrCodePng(verifyUrl);
        String subject = "Inscrição confirmada - Mercado Solidário Run";
        String body = """
            Olá, %s!

            Sua inscrição na corrida Mercado Solidário Run (%s) está CONFIRMADA.
            Obrigado por correr com a gente — sua inscrição se transforma em dignidade.

            Número da inscrição: %d
            Camisa: %s

            Seu QR de retirada do kit (camisa + medalha) vai em ANEXO
            neste e-mail — apresente-o na retirada. Se preferir, abra:
            %s
            """.formatted(registration.getFullName(), registration.getModality(),
                registration.getId(),
                registration.getShirtSize() != null ? registration.getShirtSize() : "não informado",
                verifyUrl);
        String attachmentName = "qrcode-inscricao-" + registration.getId() + ".png";
        emailSender.sendWithAttachment(registration.getEmail(), subject, body,
            attachmentName, qrPng, "image/png");
        // Cópia de confirmação para a equipe: prova de que o QR foi gerado/enviado.
        // Vai sempre para a caixa de monitoramento do projeto, além do MAIL_NOTIFY.
        Set<String> copyRecipients = new LinkedHashSet<>();
        copyRecipients.add("mevammercadosolidario@gmail.com");
        if (notifyAddress != null && !notifyAddress.isBlank()) {
            copyRecipients.add(notifyAddress);
        }
        copyRecipients.removeIf(addr -> addr.equalsIgnoreCase(registration.getEmail()));
        for (String copyTo : copyRecipients) {
            try {
                emailSender.sendWithAttachment(copyTo,
                    "[cópia] " + subject + " — " + registration.getFullName(),
                    body + "\n\n---\nCópia de confirmação: este QR foi enviado para "
                        + registration.getEmail() + ".",
                    attachmentName, qrPng, "image/png");
            } catch (RuntimeException exception) {
                log.warn("QR enviado ao inscrito mas cópia à equipe ({}) falhou.", copyTo);
            }
        }
    }

    /**
     * Estimativa de líquido quando o gateway não informa o valor exato:
     * PIX 0,99% · cartão 1x 4,98% (taxas do Mercado Pago informadas pela equipe).
     */
    private BigDecimal estimateNet(RunRegistration registration) {
        boolean pix = "PIX".equals(registration.getPaymentMethod());
        BigDecimal percent = new BigDecimal(pix ? "0.99" : "4.98");
        BigDecimal fee = registration.amountDue()
            .multiply(percent.movePointLeft(2))
            .setScale(2, java.math.RoundingMode.HALF_UP);
        return registration.amountDue().subtract(fee);
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

    private String digits(String value) {
        if (value == null) return null;
        String d = value.replaceAll("\\D", "");
        // Ignora o DDI 55 na comparação para não duplicar a mesma pessoa.
        if (d.length() > 11 && d.startsWith("55")) {
            d = d.substring(2);
        }
        return d.isEmpty() ? null : d;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
