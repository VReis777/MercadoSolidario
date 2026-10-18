package com.mercadosolidario.service;

import com.mercadosolidario.dto.publicrequest.CreatePublicServiceRequest;
import com.mercadosolidario.dto.publicrequest.PublicServiceRequestCreatedResponse;
import com.mercadosolidario.dto.publicrequest.ServiceRequestLookupRequest;
import com.mercadosolidario.dto.publicrequest.ServiceRequestLookupResponse;
import com.mercadosolidario.model.PublicServiceRequest;
import com.mercadosolidario.model.PublicServiceRequestHistory;
import com.mercadosolidario.model.ServiceRequestContactPreference;
import com.mercadosolidario.model.ServiceRequestStatus;
import com.mercadosolidario.model.ServiceRequestWelcomePreference;
import com.mercadosolidario.repository.PublicServiceRequestHistoryRepository;
import com.mercadosolidario.repository.PublicServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicServiceRequestService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final PublicServiceRequestRepository repository;
    private final PublicServiceRequestHistoryRepository historyRepository;
    private final WelcomeAvailabilityService welcomeAvailabilityService;
    private final InvitationEmailSender emailSender;

    @Value("${app.mail.notify:}")
    private String notifyAddress;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Transactional
    public PublicServiceRequestCreatedResponse create(CreatePublicServiceRequest request) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String protocol = HEX.formatHex(randomBytes);

        PublicServiceRequest entity = new PublicServiceRequest();
        entity.setProtocolHash(sha256(protocol));
        entity.setFullName(request.getFullName());
        entity.setContactValue(request.getContactValue());
        entity.setContactPreference(request.getContactPreference());
        entity.setLocation(request.getLocation());
        entity.setHouseholdSize(request.getHouseholdSize());
        entity.setAssistanceType(request.getAssistanceType());
        entity.setMessage(request.getMessage());
        entity.setConsentAccepted(request.getConsentAccepted());
        if (request.getWelcomePreference() == ServiceRequestWelcomePreference.VISIT) {
            if (!welcomeAvailabilityService.isAvailableForChoice(request.getWelcomeDate())) {
                throw new IllegalArgumentException(
                    "O dia escolhido para conversar não está disponível. Escolha uma das datas oferecidas.");
            }
            entity.setWelcomePreference(ServiceRequestWelcomePreference.VISIT);
            entity.setWelcomeDate(request.getWelcomeDate());
            entity.setWelcomeTime(request.getWelcomeTime());
        } else if (request.getWelcomePreference() == ServiceRequestWelcomePreference.PHONE) {
            entity.setWelcomePreference(ServiceRequestWelcomePreference.PHONE);
        }
        repository.save(entity);
        PublicServiceRequestHistory history = new PublicServiceRequestHistory();
        history.setServiceRequest(entity);
        history.setAction("CREATED");
        history.setNewStatus(ServiceRequestStatus.RECEBIDA);
        history.setActorUsername("PUBLIC");
        String welcomeNote = entity.getWelcomePreference() == ServiceRequestWelcomePreference.VISIT
            ? " Acolhimento presencial em " + entity.getWelcomeDate()
                + (entity.getWelcomeTime() != null ? " às " + entity.getWelcomeTime() : "") + "."
            : entity.getWelcomePreference() == ServiceRequestWelcomePreference.PHONE
                ? " Prefere acolhimento por telefone."
                : "";
        history.setObservation("Solicitação recebida." + welcomeNote);
        historyRepository.save(history);
        notifyTeam(entity);

        return new PublicServiceRequestCreatedResponse(protocol);
    }

    @Transactional(readOnly = true)
    public ServiceRequestLookupResponse lookup(ServiceRequestLookupRequest request) {
        String normalizedProtocol = ServiceRequestProtocol.normalize(request.protocol());
        if (!normalizedProtocol.matches("[0-9a-f]{64}")) {
            throw notFound();
        }
        String expectedHash = ServiceRequestProtocol.hash(normalizedProtocol);
        PublicServiceRequest entity = repository.findByProtocolHash(expectedHash)
            .filter(found -> ServiceRequestProtocol.constantTimeEquals(expectedHash, found.getProtocolHash()))
            .filter(found -> contactMatches(found, request.contactValue()))
            .orElseThrow(PublicServiceRequestService::notFound);

        return new ServiceRequestLookupResponse(
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            maskContact(entity.getContactValue(), entity.getContactPreference())
        );
    }

    private boolean contactMatches(PublicServiceRequest entity, String supplied) {
        String stored = entity.getContactValue();
        if (stored == null || stored.isBlank() || supplied == null || supplied.isBlank()) {
            return false;
        }
        String expected;
        String actual;
        if (entity.getContactPreference() == ServiceRequestContactPreference.EMAIL) {
            expected = stored.trim().toLowerCase(Locale.ROOT);
            actual = supplied.trim().toLowerCase(Locale.ROOT);
        } else if (entity.getContactPreference() == ServiceRequestContactPreference.PHONE) {
            expected = stored.replaceAll("[^0-9]", "");
            actual = supplied.replaceAll("[^0-9]", "");
        } else {
            expected = stored.trim().toLowerCase(Locale.ROOT);
            actual = supplied.trim().toLowerCase(Locale.ROOT);
        }
        return !expected.isEmpty() && ServiceRequestProtocol.constantTimeEquals(expected, actual);
    }

    public static String maskContact(String value, ServiceRequestContactPreference preference) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (preference == ServiceRequestContactPreference.EMAIL) {
            int at = value.indexOf('@');
            if (at > 0) {
                return value.substring(0, 1) + "***" + value.substring(at);
            }
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() >= 2) {
            return "••••••" + digits.substring(digits.length() - 2);
        }
        return "••••••";
    }

    private static com.mercadosolidario.exception.ResourceNotFoundException notFound() {
        return new com.mercadosolidario.exception.ResourceNotFoundException("Solicitação não encontrada");
    }

    private void notifyTeam(PublicServiceRequest request) {
        String recipient = notifyAddress != null && !notifyAddress.isBlank() ? notifyAddress : fromAddress;
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        try {
            emailSender.sendWithCopy(recipient, null,
                "Nova solicitação de atendimento - Mercado Solidário",
                """
                Uma nova solicitação de atendimento chegou pelo site.

                Nome: %s
                Contato informado: %s (%s)
                Localidade: %s
                Pessoas na residência: %s
                Tipo de apoio: %s
                Acolhimento: %s

                Mensagem da pessoa:
                %s

                A solicitação está na fila do sistema para triagem.
                """.formatted(
                    request.getFullName(),
                    request.getContactValue(), request.getContactPreference(),
                    request.getLocation() != null ? request.getLocation() : "não informada",
                    request.getHouseholdSize() != null ? request.getHouseholdSize() : "não informado",
                    request.getAssistanceType(),
                    request.getWelcomePreference() == ServiceRequestWelcomePreference.VISIT
                        ? "presencial em " + request.getWelcomeDate()
                            + (request.getWelcomeTime() != null ? " às " + request.getWelcomeTime() : "")
                        : request.getWelcomePreference() == ServiceRequestWelcomePreference.PHONE
                            ? "prefere contato por telefone" : "sem preferência",
                    request.getMessage() != null ? request.getMessage() : "(sem mensagem)"));
        } catch (RuntimeException exception) {
            log.warn("Service request saved but team notification e-mail was not sent.");
        }
    }

    private String sha256(String value) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
