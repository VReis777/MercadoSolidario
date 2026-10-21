package com.mercadosolidario.service;

import com.mercadosolidario.dto.distribution.*;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.exception.InsufficientStockException;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.*;
import com.mercadosolidario.model.enums.DistributionStatus;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DistributionService {

    private final DistributionRepository distributionRepository;
    private final DistributionItemRepository itemRepository;
    private final FamilyRepository familyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final StockService stockService;

    @Transactional
    public DistributionResponse create(CreateDistributionRequest request) {
        Distribution existing = distributionRepository.findByIdempotencyKey(request.getIdempotencyKey().trim())
            .orElse(null);
        if (existing != null) {
            if (!sameRequest(existing, request)) {
                throw new ConflictException("A chave de idempotência já foi utilizada em outra distribuição.");
            }
            return toResponse(existing);
        }

        Family family = familyRepository.findById(request.getFamilyId())
            .orElseThrow(() -> new ResourceNotFoundException("Família não encontrada"));
        if (family.getStatus() == FamilyStatus.INATIVA || family.getStatus() == FamilyStatus.CANCELADA) {
            throw new ConflictException("Não é possível registrar distribuição para uma família inativa ou cancelada.");
        }
        if (family.getStatus() == FamilyStatus.CONTEMPLADA || family.getStatus() == FamilyStatus.ATENDIDA) {
            throw new ConflictException("Esta família já foi contemplada e não pode receber uma segunda retirada.");
        }
        validateUniqueProducts(request);

        List<Long> productIds = request.getItems().stream()
            .map(DistributionItemRequest::getProductId)
            .sorted()
            .toList();
        List<Product> lockedProducts = productRepository.findAllByIdForUpdate(productIds);
        if (lockedProducts.size() != productIds.size()) {
            Set<Long> foundIds = lockedProducts.stream().map(Product::getId).collect(Collectors.toSet());
            Long missing = productIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new ResourceNotFoundException("Produto não encontrado: " + missing);
        }

        Map<Long, Product> products = lockedProducts.stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (DistributionItemRequest item : request.getItems()) {
            Product product = products.get(item.getProductId());
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new ConflictException("Produto inativo não pode ser distribuído: " + product.getName());
            }
            int balance = product.getCurrentQuantity() == null ? 0 : product.getCurrentQuantity();
            if (balance < item.getQuantity()) {
                throw new InsufficientStockException("Estoque insuficiente para " + product.getName()
                    + ". Disponível: " + balance + " " + product.getUnit() + ".");
            }
        }

        String username = actor();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalStateException("Usuário autenticado não foi encontrado."));
        LocalDateTime now = LocalDateTime.now();
        Distribution distribution = new Distribution();
        distribution.setFamily(family);
        distribution.setUser(user);
        distribution.setCreatedBy(username);
        distribution.setStatus(DistributionStatus.CONFIRMADA);
        distribution.setDistributionDate(now);
        distribution.setCompletedAt(now);
        distribution.setObservations(trimToNull(request.getObservation()));
        distribution.setReference(trimToNull(request.getReference()));
        distribution.setIdempotencyKey(request.getIdempotencyKey().trim());
        Distribution saved;
        try {
            saved = distributionRepository.saveAndFlush(distribution);
        } catch (DataIntegrityViolationException exception) {
            if (isIdempotencyConflict(exception)) {
                throw new ConflictException("A chave de idempotência já foi utilizada em outra distribuição.");
            }
            throw exception;
        }

        List<DistributionItem> savedItems = new ArrayList<>();
        for (DistributionItemRequest itemRequest : request.getItems()) {
            Product product = products.get(itemRequest.getProductId());
            StockMovement movement = stockService.registerDistributionMovement(
                product.getId(), itemRequest.getQuantity(), family,
                "Distribuição " + saved.getId());

            DistributionItem item = new DistributionItem();
            item.setDistribution(saved);
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnit(product.getUnit());
            item.setStockMovement(movement);
            savedItems.add(itemRepository.save(item));
        }

        family.setStatus(FamilyStatus.CONTEMPLADA);
        familyRepository.save(family);

        String auditDetails = "familyId=" + family.getId() + ",items=" + describeItems(savedItems);
        audit("DISTRIBUTION_CREATED", saved.getId(), username, auditDetails);
        audit("DISTRIBUTION_CONFIRMED", saved.getId(), username, auditDetails);
        saved.setItems(savedItems);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DistributionResponse> list() {
        return distributionRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DistributionResponse get(Long id) {
        Distribution distribution = distributionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Distribuição não encontrada"));
        return toResponse(distribution);
    }

    @Transactional(readOnly = true)
    public List<DistributionResponse> byFamily(Long familyId) {
        if (!familyRepository.existsById(familyId)) {
            throw new ResourceNotFoundException("Família não encontrada");
        }
        return distributionRepository.findByFamilyIdOrderByDistributionDateDesc(familyId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DistributionResponse cancel(Long id, CancelDistributionRequest request) {
        Distribution distribution = distributionRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Distribuição não encontrada"));
        if (distribution.getStatus() != DistributionStatus.CONFIRMADA) {
            throw new ConflictException("Somente uma distribuição confirmada pode ser cancelada.");
        }
        String username = actor();
        distribution.setStatus(DistributionStatus.CANCELADA);
        distribution.setCancellationReason(request.getReason().trim());
        Family family = distribution.getFamily();
        if (family.getStatus() == FamilyStatus.CONTEMPLADA) {
            family.setStatus(FamilyStatus.APROVADA);
            familyRepository.save(family);
        }
        Distribution saved = distributionRepository.save(distribution);
        audit("DISTRIBUTION_CANCELLED", saved.getId(), username,
            "familyId=" + saved.getFamily().getId() + ",reason=" + request.getReason().trim()
                + ",stockReturned=false");
        return toResponse(saved);
    }

    private void validateUniqueProducts(CreateDistributionRequest request) {
        Set<Long> ids = new HashSet<>();
        for (DistributionItemRequest item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("A quantidade de cada produto deve ser maior que zero.");
            }
            if (!ids.add(item.getProductId())) {
                throw new IllegalArgumentException("Informe cada produto uma única vez na distribuição.");
            }
        }
    }

    private boolean sameRequest(Distribution existing, CreateDistributionRequest request) {
        if (!Objects.equals(existing.getFamily().getId(), request.getFamilyId())
                || !Objects.equals(trimToNull(existing.getObservations()), trimToNull(request.getObservation()))
                || !Objects.equals(trimToNull(existing.getReference()), trimToNull(request.getReference()))) {
            return false;
        }
        List<DistributionItem> existingItems = itemRepository.findByDistributionIdOrderByIdAsc(existing.getId());
        if (existingItems.size() != request.getItems().size()) return false;
        Map<Long, Integer> existingByProduct = existingItems.stream().collect(
            Collectors.toMap(item -> item.getProduct().getId(), DistributionItem::getQuantity));
        return request.getItems().stream().allMatch(item ->
            Objects.equals(existingByProduct.get(item.getProductId()), item.getQuantity()));
    }

    private DistributionResponse toResponse(Distribution distribution) {
        List<DistributionItem> items = itemRepository.findByDistributionIdOrderByIdAsc(distribution.getId());
        List<DistributionItemResponse> itemResponses = items.stream().map(item ->
            new DistributionItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnit(),
                item.getStockMovement() == null ? null : item.getStockMovement().getId()
            )
        ).toList();
        return new DistributionResponse(
            distribution.getId(),
            distribution.getFamily().getId(),
            distribution.getFamily().getResponsibleName(),
            distribution.getStatus(),
            distribution.getCreatedBy() != null
                ? distribution.getCreatedBy()
                : distribution.getUser() == null ? null : distribution.getUser().getUsername(),
            distribution.getCreatedAt(),
            distribution.getCompletedAt(),
            distribution.getObservations(),
            distribution.getReference(),
            distribution.getCancellationReason(),
            itemResponses
        );
    }

    private String describeItems(List<DistributionItem> items) {
        return items.stream()
            .map(item -> item.getProduct().getId() + ":" + item.getQuantity() + ":" + item.getUnit())
            .collect(Collectors.joining(";"));
    }

    private void audit(String action, Long id, String username, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType("Distribution");
        entry.setEntityId(id);
        entry.setDetails(details);
        userRepository.findByUsername(username).ifPresent(entry::setUser);
        auditLogRepository.save(entry);
    }

    private String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("Usuário autenticado obrigatório para esta operação.");
        }
        return authentication.getName();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isIdempotencyConflict(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && (message.contains("uq_distributions_idempotency_key")
                    || message.contains("idempotency_key"))) {
                return true;
            }
        }
        return false;
    }
}
