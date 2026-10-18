package com.mercadosolidario;

import com.mercadosolidario.dto.stock.ProductRequest;
import com.mercadosolidario.dto.stock.StockMovementRequest;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.exception.InsufficientStockException;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.model.StockMovement;
import com.mercadosolidario.model.enums.MovementType;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.ProductRepository;
import com.mercadosolidario.repository.StockMovementRepository;
import com.mercadosolidario.repository.UserRepository;
import com.mercadosolidario.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private StockService service;

    @BeforeEach
    void setUp() {
        service = new StockService(productRepository, movementRepository, userRepository, auditLogRepository);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin-test", "N/A", List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProductStartsWithZeroBalanceAndAuditsTheAction() {
        ProductRequest request = new ProductRequest();
        request.setName("Arroz");
        request.setCategory("Alimentos");
        request.setUnit("pacote");
        request.setMinimumQuantity(4);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(12L);
            return product;
        });
        when(userRepository.findByUsername("admin-test")).thenReturn(Optional.empty());

        var result = service.createProduct(request);

        assertThat(result.id()).isEqualTo(12L);
        assertThat(result.currentQuantity()).isZero();
        assertThat(result.minimumQuantity()).isEqualTo(4);
        verify(auditLogRepository).save(argThat(audit -> audit.getAction().equals("PRODUCT_CREATED")));
    }

    @Test
    void movementUpdatesBalanceAndPersistsActorAndBalanceHistory() {
        Product product = product(8);
        StockMovementRequest request = movementRequest(MovementType.PERDA, 3);
        when(productRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(product));
        when(userRepository.findByUsername("admin-test")).thenReturn(Optional.empty());
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.registerMovement(request);

        assertThat(response.previousBalance()).isEqualTo(8);
        assertThat(response.newBalance()).isEqualTo(5);
        assertThat(response.username()).isEqualTo("admin-test");
        assertThat(product.getCurrentQuantity()).isEqualTo(5);
        verify(productRepository).save(product);
        verify(movementRepository).save(argThat(movement ->
            movement.getPreviousBalance() == 8
                && movement.getNewBalance() == 5
                && movement.getActorUsername().equals("admin-test")));
        verify(auditLogRepository).save(argThat(audit -> audit.getAction().equals("STOCK_LOSS_RECORDED")));
    }

    @Test
    void insufficientBalanceDoesNotPersistAPartialMovement() {
        Product product = product(2);
        when(productRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.registerMovement(movementRequest(MovementType.PERDA, 3)))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("Estoque insuficiente");

        assertThat(product.getCurrentQuantity()).isEqualTo(2);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(movementRepository, auditLogRepository);
    }

    @Test
    void rejectsZeroOrNegativeMovementQuantity() {
        Product product = product(4);
        when(productRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.registerMovement(movementRequest(MovementType.ENTRADA_COMPRA, 0)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registerMovement(movementRequest(MovementType.ENTRADA_COMPRA, -1)))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(movementRepository, auditLogRepository);
    }

    @Test
    void missingProductReturnsNotFound() {
        when(productRepository.findByIdForUpdate(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerMovement(movementRequest(MovementType.ENTRADA_DOACAO, 1)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Produto não encontrado");
        verifyNoInteractions(movementRepository, auditLogRepository);
    }

    @Test
    void inactiveProductCannotBeChangedByMovementAndHistoryRemainsReadable() {
        Product product = product(4);
        product.setActive(false);
        when(productRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.registerMovement(movementRequest(MovementType.ENTRADA_DOACAO, 1)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Produto inativo");

        verifyNoInteractions(movementRepository, auditLogRepository);
    }

    private Product product(int balance) {
        Product product = new Product();
        product.setId(12L);
        product.setName("Arroz");
        product.setUnit("pacote");
        product.setCurrentQuantity(balance);
        product.setMinimumQuantity(2);
        product.setActive(true);
        return product;
    }

    private StockMovementRequest movementRequest(MovementType type, int quantity) {
        StockMovementRequest request = new StockMovementRequest();
        request.setProductId(12L);
        request.setType(type);
        request.setQuantity(quantity);
        request.setReason("Conferência de estoque");
        return request;
    }
}
