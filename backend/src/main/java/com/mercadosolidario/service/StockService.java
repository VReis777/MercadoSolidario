package com.mercadosolidario.service;

import com.mercadosolidario.dto.stock.*;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.exception.InsufficientStockException;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.AuditLog;
import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.model.StockMovement;
import com.mercadosolidario.model.User;
import com.mercadosolidario.model.enums.MovementType;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.ProductRepository;
import com.mercadosolidario.repository.StockMovementRepository;
import com.mercadosolidario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(boolean includeInactive) {
        List<Product> products = includeInactive
            ? productRepository.findAll()
            : productRepository.findByActiveTrue();
        return products.stream().map(this::toProductResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return toProductResponse(requireProduct(id));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        product.setCurrentQuantity(0);
        product.setActive(true);
        Product saved = productRepository.save(product);
        audit("PRODUCT_CREATED", "Product", saved.getId(), actor());
        return toProductResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        apply(product, request);
        Product saved = productRepository.save(product);
        audit("PRODUCT_UPDATED", "Product", saved.getId(), actor());
        return toProductResponse(saved);
    }

    @Transactional
    public ProductResponse updateStatus(Long id, ProductStatusRequest request) {
        Product product = productRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        product.setActive(request.active());
        Product saved = productRepository.save(product);
        audit(request.active() ? "PRODUCT_ACTIVATED" : "PRODUCT_DEACTIVATED",
            "Product", saved.getId(), actor());
        return toProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public StockSummaryResponse summary() {
        List<Product> activeProducts = productRepository.findByActiveTrue();
        List<ProductResponse> response = activeProducts.stream().map(this::toProductResponse).toList();
        long lowStock = response.stream().filter(ProductResponse::lowStock).count();
        return new StockSummaryResponse(activeProducts.size(), lowStock, response);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> lowStock() {
        return productRepository.findLowStockProducts().stream().map(this::toProductResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> productMovements(Long productId) {
        requireProduct(productId);
        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId)
            .stream().map(this::toMovementResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> recentMovements() {
        return movementRepository.findTop200ByOrderByCreatedAtDescIdDesc()
            .stream().map(this::toMovementResponse).toList();
    }

    @Transactional
    public StockMovementResponse registerMovement(StockMovementRequest request) {
        return toMovementResponse(registerMovement(request, null));
    }

    @Transactional
    public StockMovement registerDistributionMovement(Long productId, Integer quantity, Family family, String reason) {
        StockMovementRequest request = new StockMovementRequest();
        request.setProductId(productId);
        request.setType(MovementType.SAIDA_DISTRIBUICAO);
        request.setQuantity(quantity);
        request.setReason(reason);
        return registerMovement(request, family);
    }

    private StockMovement registerMovement(StockMovementRequest request, Family family) {
        Product product = productRepository.findByIdForUpdate(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ConflictException("Produto inativo não pode receber movimentações.");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("A quantidade movimentada deve ser maior que zero.");
        }
        boolean inbound = isInbound(request);
        if (request.getType() == MovementType.AJUSTE && request.getAdjustmentDirection() == null) {
            throw new IllegalArgumentException("Informe se o ajuste aumenta ou reduz o saldo.");
        }
        int previous = product.getCurrentQuantity() == null ? 0 : product.getCurrentQuantity();
        long nextBalance = inbound
            ? (long) previous + request.getQuantity()
            : (long) previous - request.getQuantity();
        if (nextBalance < 0) {
            throw new InsufficientStockException("Estoque insuficiente para esta movimentação.");
        }
        if (nextBalance > Integer.MAX_VALUE) {
            throw new ConflictException("A quantidade resultante excede o limite suportado.");
        }

        String username = actor();
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setFamily(family);
        movement.setType(request.getType());
        movement.setQuantity(request.getQuantity());
        movement.setPreviousBalance(previous);
        movement.setNewBalance((int) nextBalance);
        movement.setActorUsername(username);
        userRepository.findByUsername(username).ifPresent(movement::setUser);
        movement.setReason(request.getReason().trim());
        movement.setObservation(trimToNull(request.getObservation()));

        product.setCurrentQuantity((int) nextBalance);
        productRepository.save(product);
        StockMovement saved = movementRepository.save(movement);
        audit(auditAction(request.getType()), "StockMovement", saved.getId(),
            "username=" + username + ",productId=" + product.getId()
                + ",previous=" + previous + ",quantity=" + request.getQuantity()
                + ",new=" + nextBalance + ",type=" + request.getType());
        return saved;
    }

    private boolean isInbound(StockMovementRequest request) {
        return switch (request.getType()) {
            case ENTRADA_DOACAO, ENTRADA_COMPRA, DEVOLUCAO -> true;
            case SAIDA_DISTRIBUICAO, PERDA -> false;
            case AJUSTE -> request.getAdjustmentDirection() == StockMovementRequest.AdjustmentDirection.ENTRADA;
        };
    }

    private String auditAction(MovementType type) {
        return switch (type) {
            case ENTRADA_DOACAO -> "STOCK_DONATION_RECEIVED";
            case ENTRADA_COMPRA -> "STOCK_PURCHASE_RECEIVED";
            case SAIDA_DISTRIBUICAO -> "STOCK_DISTRIBUTION_ISSUED";
            case PERDA -> "STOCK_LOSS_RECORDED";
            case DEVOLUCAO -> "STOCK_RETURN_RECEIVED";
            case AJUSTE -> "STOCK_ADJUSTED";
        };
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.getName().trim());
        product.setCategory(trimToNull(request.getCategory()));
        product.setUnit(request.getUnit().trim());
        product.setMinimumQuantity(request.getMinimumQuantity() == null ? 0 : request.getMinimumQuantity());
        product.setBasketQuantity(request.getBasketQuantity());
        if (request.getNotes() != null) {
            product.setNotes(trimToNull(request.getNotes()));
        }
    }

    private Product requireProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
    }

    private ProductResponse toProductResponse(Product product) {
        int current = product.getCurrentQuantity() == null ? 0 : product.getCurrentQuantity();
        int minimum = product.getMinimumQuantity() == null ? 0 : product.getMinimumQuantity();
        return new ProductResponse(
            product.getId(), product.getName(), product.getCategory(), product.getUnit(),
            current, minimum, product.getBasketQuantity(),
            Boolean.TRUE.equals(product.getActive()), current <= minimum,
            product.getCreatedAt(), product.getUpdatedAt(), product.getNotes()
        );
    }

    private StockMovementResponse toMovementResponse(StockMovement movement) {
        String username = movement.getActorUsername();
        if (username == null && movement.getUser() != null) {
            username = movement.getUser().getUsername();
        }
        return new StockMovementResponse(
            movement.getId(), movement.getProduct().getId(), movement.getProduct().getName(),
            movement.getType(), movement.getQuantity(), movement.getPreviousBalance(),
            movement.getNewBalance(), username, movement.getReason(), movement.getObservation(),
            movement.getCreatedAt()
        );
    }

    private void audit(String action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        String username = actor();
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
}
