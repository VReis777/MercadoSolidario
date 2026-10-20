package com.mercadosolidario.service;

import com.mercadosolidario.model.AuditLog;
import com.mercadosolidario.model.CashMovement;
import com.mercadosolidario.model.enums.CashMovementType;
import com.mercadosolidario.model.enums.DonationStatus;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.CashMovementRepository;
import com.mercadosolidario.repository.DonationRepository;
import com.mercadosolidario.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashMovementService {

    private static final long MAX_ATTACHMENT_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "application/pdf", "image/jpeg", "image/png");

    private final CashMovementRepository cashMovementRepository;
    private final DonationRepository donationRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.mercadosolidario.repository.UserRepository userRepository;
    private final InvitationEmailSender emailSender;

    @Value("${app.mail.notify:${app.mail.from:}}")
    private String notifyAddress;

    @Transactional(readOnly = true)
    public CashSummary summary() {
        BigDecimal donationsTotal = donationRepository.sumConfirmedAmount();
        BigDecimal movementsBalance = cashMovementRepository.balance();
        BigDecimal total = donationsTotal.add(movementsBalance);
        List<CashMovement> movements = cashMovementRepository.findAllByOrderByCreatedAtDescIdDesc();
        return new CashSummary(total, donationsTotal, movementsBalance, movements.stream().map(this::toItem).toList());
    }

    @Transactional
    public CashMovement create(String type, BigDecimal amount, String description, MultipartFile file) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Informe um valor maior que zero.");
        }
        CashMovementType movementType;
        try {
            movementType = CashMovementType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Tipo deve ser ENTRADA ou SAIDA.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }

        CashMovement movement = new CashMovement();
        movement.setType(movementType);
        movement.setAmount(amount);
        movement.setDescription(description.trim());
        movement.setCreatedBy(currentActor());

        if (file != null && !file.isEmpty()) {
            if (file.getSize() > MAX_ATTACHMENT_SIZE) {
                throw new IllegalArgumentException("Anexo deve ter no máximo 5 MB.");
            }
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
                throw new IllegalArgumentException("Anexo deve ser PDF, JPG ou PNG.");
            }
            try {
                movement.setAttachmentName(file.getOriginalFilename() == null ? "comprovante" : file.getOriginalFilename());
                movement.setAttachmentContentType(contentType);
                movement.setAttachmentData(file.getBytes());
            } catch (IOException e) {
                throw new IllegalStateException("Não foi possível ler o anexo.", e);
            }
        }

        movement = cashMovementRepository.save(movement);
        audit(movement);
        notifyByEmail(movement);
        return movement;
    }

    @Transactional(readOnly = true)
    public CashMovement attachment(Long id) {
        CashMovement movement = cashMovementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movimentação não encontrada"));
        if (movement.getAttachmentData() == null) {
            throw new ResourceNotFoundException("Movimentação sem anexo.");
        }
        return movement;
    }

    private void notifyByEmail(CashMovement movement) {
        if (notifyAddress == null || notifyAddress.isBlank()) {
            return;
        }
        try {
            String direction = movement.getType() == CashMovementType.ENTRADA ? "Entrada" : "Saída";
            emailSender.sendWithAttachment(notifyAddress,
                direction + " no caixa - Mercado Solidário",
                "%s de R$ %.2f registrada por %s.\n\nDescrição: %s\n%s".formatted(
                    direction, movement.getAmount(), movement.getCreatedBy(),
                    movement.getDescription(),
                    movement.getAttachmentName() != null
                        ? "Comprovante anexado a este e-mail." : "Sem comprovante anexado."),
                movement.getAttachmentName(), movement.getAttachmentData(),
                movement.getAttachmentContentType());
        } catch (Exception exception) {
            log.warn("Movimentação de caixa salva, mas e-mail com comprovante não foi enviado.");
        }
    }

    private void audit(CashMovement movement) {
        AuditLog audit = new AuditLog();
        audit.setAction("CASH_MOVEMENT_CREATED");
        audit.setEntityType("CashMovement");
        audit.setEntityId(movement.getId());
        audit.setDetails("{\"type\":\"" + movement.getType() + "\",\"amount\":" + movement.getAmount() + "}");
        userRepository.findByUsername(movement.getCreatedBy()).ifPresent(audit::setUser);
        auditLogRepository.save(audit);
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Usuário autenticado obrigatório.");
        }
        return authentication.getName();
    }

    private CashItem toItem(CashMovement m) {
        return new CashItem(m.getId(), m.getType().name(), m.getAmount(), m.getDescription(),
            m.getAttachmentName() != null, m.getCreatedBy(), m.getCreatedAt());
    }

    public record CashSummary(BigDecimal total, BigDecimal donationsTotal,
                              BigDecimal movementsBalance, List<CashItem> movements) {}

    public record CashItem(Long id, String type, BigDecimal amount, String description,
                           boolean hasAttachment, String createdBy, java.time.LocalDateTime createdAt) {}
}
