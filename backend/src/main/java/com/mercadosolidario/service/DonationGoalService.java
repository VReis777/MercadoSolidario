package com.mercadosolidario.service;

import com.mercadosolidario.dto.donation.*;
import com.mercadosolidario.dto.stock.StockMovementRequest;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.*;
import com.mercadosolidario.model.enums.DonationStatus;
import com.mercadosolidario.model.enums.DonationType;
import com.mercadosolidario.model.enums.MovementType;
import com.mercadosolidario.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationGoalService {

    private static final String FAMILIES_GOAL_KEY = "donation_goal_families";
    private static final int DEFAULT_FAMILIES_GOAL = 500;

    private final DonationGoalItemRepository goalItemRepository;
    private final DonationRepository donationRepository;
    private final ProductRepository productRepository;
    private final SystemSettingRepository settingRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final StockService stockService;
    private final InvitationEmailSender emailSender;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Transactional(readOnly = true)
    public List<DonationGoalItemResponse> publicSummary() {
        int families = familiesGoal();
        List<DonationGoalItem> items = goalItemRepository.findByActiveTrueOrderBySortOrderAsc();
        // As somas vêm em PACOTES (doações ligadas a itens gravam o nº de pacotes);
        // converter para a unidade do item multiplicando pelo tamanho da embalagem.
        Map<Long, BigDecimal> pledgedPackages = sumByStatus(List.of(DonationStatus.PROMETIDA));
        Map<Long, BigDecimal> receivedPackages = sumByStatus(
            List.of(DonationStatus.CONFIRMADA_MANUALMENTE, DonationStatus.PAGA));
        return items.stream().map(item -> {
            BigDecimal needed = item.getPerFamilyQuantity().multiply(BigDecimal.valueOf(families));
            BigDecimal pledgedQty = pledgedPackages.getOrDefault(item.getId(), BigDecimal.ZERO)
                .multiply(item.getPackageSize());
            BigDecimal receivedQty = receivedPackages.getOrDefault(item.getId(), BigDecimal.ZERO)
                .multiply(item.getPackageSize());
            BigDecimal remaining = needed.subtract(pledgedQty).subtract(receivedQty).max(BigDecimal.ZERO);
            long packages = remaining.compareTo(BigDecimal.ZERO) <= 0
                ? 0
                : remaining.divide(item.getPackageSize(), 0, RoundingMode.CEILING).longValue();
            return new DonationGoalItemResponse(
                item.getId(), item.getName(), item.getUnit(),
                item.getPerFamilyQuantity(), item.getPackageSize(),
                needed, pledgedQty, receivedQty, remaining, packages,
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getCurrentQuantity() : null);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<String> confirmedDonors() {
        return donationRepository.findDistinctConfirmedProductDonors(DonationStatus.CONFIRMADA_MANUALMENTE);
    }

    @Transactional
    public DonationPledgeResponse pledge(DonationPledgeRequest request) {
        DonationGoalItem item = goalItemRepository.findById(request.goalItemId())
            .filter(DonationGoalItem::getActive)
            .orElseThrow(() -> new ResourceNotFoundException("Item de doação não encontrado"));
        if (!request.selfDelivery() && trimToNull(request.donorContact()) == null) {
            throw new IllegalArgumentException(
                "Informe um contato ou marque que você mesmo levará a doação até o MEVAM Petrópolis.");
        }

        Donation donation = new Donation();
        donation.setType(DonationType.PRODUTO);
        donation.setGoalItem(item);
        donation.setProduct(item.getProduct());
        // quantity = número de pacotes prometidos (embalagem do item da meta).
        donation.setQuantity(request.packages());
        donation.setUnit(item.getUnit());
        donation.setIsAnonymous(request.anonymous());
        donation.setSelfDelivery(request.selfDelivery());
        donation.setDonorName(request.anonymous() ? null : trimToNull(request.donorName()));
        donation.setDonorContact(trimToNull(request.donorContact()));
        donation.setStatus(DonationStatus.PROMETIDA);
        Donation saved = donationRepository.save(donation);
        audit("DONATION_PLEDGE_CREATED", saved.getId(),
            "goalItemId=" + item.getId() + ",packages=" + request.packages()
                + ",selfDelivery=" + request.selfDelivery());
        notifyTeam(saved, item);
        return toPledgeResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DonationPledgeResponse> pendingPledges() {
        return donationRepository.findByStatusOrderByCreatedAtDesc(DonationStatus.PROMETIDA)
            .stream().map(this::toPledgeResponse).toList();
    }

    @Transactional
    public DonationPledgeResponse receive(Long id, ReceivePledgeRequest request) {
        Donation donation = donationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doação não encontrada"));
        if (donation.getStatus() != DonationStatus.PROMETIDA) {
            throw new ConflictException("Somente doações prometidas podem ser recebidas.");
        }

        // Prioridade: produto gravado na doação; senão o produto ligado ao item
        // da meta; senão um productId enviado explicitamente (legado).
        Product product = donation.getProduct();
        if (product == null && donation.getGoalItem() != null) {
            product = donation.getGoalItem().getProduct();
        }
        if (product == null && request.productId() != null) {
            product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + request.productId()));
        }
        String username = actor();
        if (product != null) {
            StockMovementRequest movement = new StockMovementRequest();
            movement.setProductId(product.getId());
            movement.setType(MovementType.ENTRADA_DOACAO);
            movement.setQuantity(donation.getQuantity());
            movement.setReason("Doação de item recebida na igreja ("
                + donation.getQuantity() + " pacote(s))");
            movement.setObservation("Donation ID " + donation.getId());
            stockService.registerMovement(movement);
            donation.setProduct(product);
        }

        donation.setStatus(DonationStatus.CONFIRMADA_MANUALMENTE);
        donation.setConfirmationDate(LocalDateTime.now());
        donation.setConfirmedBy(username);
        Donation saved = donationRepository.save(donation);
        audit("DONATION_PLEDGE_RECEIVED", saved.getId(),
            "goalItemId=" + (donation.getGoalItem() != null ? donation.getGoalItem().getId() : null)
                + ",quantity=" + donation.getQuantity() + ",unit=" + donation.getUnit()
                + ",productId=" + (product != null ? product.getId() : null));
        return toPledgeResponse(saved);
    }

    @Transactional
    public DonationPledgeResponse reject(Long id) {
        Donation donation = donationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doação não encontrada"));
        if (donation.getStatus() != DonationStatus.PROMETIDA) {
            throw new ConflictException("Somente doações prometidas podem ser recusadas.");
        }
        donation.setStatus(DonationStatus.CANCELADA);
        Donation saved = donationRepository.save(donation);
        audit("DONATION_PLEDGE_REJECTED", saved.getId(),
            "goalItemId=" + (donation.getGoalItem() != null ? donation.getGoalItem().getId() : null));
        return toPledgeResponse(saved);
    }

    private int familiesGoal() {
        return settingRepository.findByKey(FAMILIES_GOAL_KEY)
            .map(s -> {
                try { return Integer.parseInt(s.getValue().trim()); }
                catch (Exception e) { return DEFAULT_FAMILIES_GOAL; }
            })
            .orElse(DEFAULT_FAMILIES_GOAL);
    }

    private Map<Long, BigDecimal> sumByStatus(List<DonationStatus> statuses) {
        return donationRepository.sumProductQuantityByGoalItemAndStatuses(statuses).stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                // SUM de coluna Integer retorna Long; pacotes são inteiros
                row -> BigDecimal.valueOf(((Number) row[1]).longValue())));
    }

    private DonationPledgeResponse toPledgeResponse(Donation donation) {
        DonationGoalItem item = donation.getGoalItem();
        return new DonationPledgeResponse(
            donation.getId(),
            item != null ? item.getId() : null,
            item != null ? item.getName() : null,
            donation.getQuantity(),
            item != null ? item.getPackageSize() : null,
            donation.getUnit(),
            donation.getDonorName(),
            donation.getDonorContact(),
            Boolean.TRUE.equals(donation.getIsAnonymous()),
            Boolean.TRUE.equals(donation.getSelfDelivery()),
            donation.getStatus(),
            donation.getCreatedAt(),
            donation.getConfirmationDate(),
            donation.getConfirmedBy()
        );
    }

    private void notifyTeam(Donation donation, DonationGoalItem item) {
        Set<String> recipients = new LinkedHashSet<>();
        userRepository.findActiveByRoleNames(List.of("ADMIN", "RH_TESOURARIA")).stream()
            .map(u -> u.getEmail())
            .filter(email -> email != null && !email.isBlank())
            .forEach(recipients::add);
        if (fromAddress != null && !fromAddress.isBlank()) {
            recipients.add(fromAddress);
        }
        if (recipients.isEmpty()) {
            return;
        }
        BigDecimal total = item.getPackageSize().multiply(BigDecimal.valueOf(donation.getQuantity()));
        try {
            emailSender.sendToMany(recipients.toArray(String[]::new),
                "Nova promessa de doação - Mercado Solidário",
                """
                Alguém prometeu doar pela página pública (nossahistoria).

                Item: %s
                Quantidade: %d pacote(s) de %s %s (total: %s %s)
                Doador: %s
                Contato: %s
                Entrega: %s

                A quantidade já foi descontada da lista pública. Confirme o
                recebimento em Doações > Promessas quando chegar à igreja.
                """.formatted(
                    item.getName(),
                    donation.getQuantity(),
                    item.getPackageSize().stripTrailingZeros().toPlainString(),
                    item.getUnit(),
                    total.stripTrailingZeros().toPlainString(),
                    item.getUnit(),
                    Boolean.TRUE.equals(donation.getIsAnonymous())
                        ? "Anônimo" : donation.getDonorName(),
                    donation.getDonorContact() != null ? donation.getDonorContact() : "não informado",
                    Boolean.TRUE.equals(donation.getSelfDelivery())
                        ? "o doador leva até o MEVAM Petrópolis"
                        : "combinar retirada com o doador"));
        } catch (RuntimeException exception) {
            log.warn("Promessa salva mas e-mail de aviso não foi enviado.");
        }
    }

    private String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "sistema";
    }

    private void audit(String action, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType("Donation");
        entry.setEntityId(entityId);
        entry.setDetails(details);
        String actorName = actor();
        if (!"sistema".equals(actorName)) {
            userRepository.findByUsername(actorName).ifPresent(entry::setUser);
        }
        auditLogRepository.save(entry);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
