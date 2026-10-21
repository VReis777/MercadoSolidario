package com.mercadosolidario;

import com.mercadosolidario.dto.distribution.CreateDistributionRequest;
import com.mercadosolidario.dto.distribution.DistributionItemRequest;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.exception.InsufficientStockException;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.*;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.repository.*;
import com.mercadosolidario.service.DistributionService;
import com.mercadosolidario.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributionServiceTest {

    @Mock private DistributionRepository distributionRepository;
    @Mock private DistributionItemRepository itemRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private StockService stockService;

    private DistributionService service;

    @BeforeEach
    void setUp() {
        service = new DistributionService(
            distributionRepository, itemRepository, familyRepository, productRepository,
            userRepository, auditLogRepository, stockService);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("diacono-test", "N/A", List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsDistributionWithMultipleItemsThroughOfficialStockService() {
        Family family = family(3L);
        Product rice = product(8L, "Arroz", 10);
        Product beans = product(4L, "Feijão", 7);
        User user = new User();
        user.setUsername("diacono-test");
        when(distributionRepository.findByIdempotencyKey("request-9")).thenReturn(Optional.empty());
        when(familyRepository.findById(3L)).thenReturn(Optional.of(family));
        when(productRepository.findAllByIdForUpdate(List.of(4L, 8L))).thenReturn(List.of(beans, rice));
        when(userRepository.findByUsername("diacono-test")).thenReturn(Optional.of(user));
        when(distributionRepository.saveAndFlush(any(Distribution.class))).thenAnswer(invocation -> {
            Distribution distribution = invocation.getArgument(0);
            distribution.setId(19L);
            distribution.setCreatedAt(LocalDateTime.now());
            return distribution;
        });
        when(stockService.registerDistributionMovement(anyLong(), anyInt(), any(Family.class), anyString()))
            .thenAnswer(invocation -> {
                StockMovement movement = new StockMovement();
                movement.setId(invocation.getArgument(0, Long.class) + 100);
                return movement;
            });
        when(itemRepository.save(any(DistributionItem.class))).thenAnswer(invocation -> {
            DistributionItem item = invocation.getArgument(0);
            item.setId(item.getProduct().getId());
            return item;
        });
        when(itemRepository.findByDistributionIdOrderByIdAsc(19L)).thenReturn(List.of());
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request(3L, "request-9", item(8L, 5), item(4L, 2)));

        assertThat(response.id()).isEqualTo(19L);
        assertThat(response.status().name()).isEqualTo("CONFIRMADA");
        assertThat(response.createdBy()).isEqualTo("diacono-test");
        verify(stockService, times(2)).registerDistributionMovement(anyLong(), anyInt(), eq(family), anyString());
        verify(stockService).registerDistributionMovement(8L, 5, family, "Distribuição 19");
        verify(stockService).registerDistributionMovement(4L, 2, family, "Distribuição 19");
        verify(familyRepository).save(argThat(saved ->
            saved.getStatus() == com.mercadosolidario.model.enums.FamilyStatus.CONTEMPLADA));
        verify(auditLogRepository, times(2)).save(argThat(log ->
            log.getAction().equals("DISTRIBUTION_CREATED")
                || log.getAction().equals("DISTRIBUTION_CONFIRMED")));
    }

    @Test
    void rejectsInsufficientBalanceBeforeAnyStockMovement() {
        Family family = family(3L);
        when(distributionRepository.findByIdempotencyKey("request-9")).thenReturn(Optional.empty());
        when(familyRepository.findById(3L)).thenReturn(Optional.of(family));
        when(productRepository.findAllByIdForUpdate(List.of(8L))).thenReturn(List.of(product(8L, "Arroz", 2)));

        assertThatThrownBy(() -> service.create(request(3L, "request-9", item(8L, 5))))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("Estoque insuficiente");

        verifyNoInteractions(stockService);
        verify(distributionRepository, never()).save(any());
        verifyNoInteractions(itemRepository, auditLogRepository);
    }

    @Test
    void familyAndProductMustExistAndProductsMustBeUnique() {
        when(distributionRepository.findByIdempotencyKey("request-9")).thenReturn(Optional.empty());
        when(familyRepository.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(request(3L, "request-9", item(8L, 1))))
            .isInstanceOf(ResourceNotFoundException.class);

        when(familyRepository.findById(3L)).thenReturn(Optional.of(family(3L)));
        assertThatThrownBy(() -> service.create(request(3L, "request-9", item(8L, 1), item(8L, 2))))
            .isInstanceOf(IllegalArgumentException.class);

        when(productRepository.findAllByIdForUpdate(List.of(8L))).thenReturn(List.of());
        assertThatThrownBy(() -> service.create(request(3L, "request-9", item(8L, 1))))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Produto não encontrado");
        verifyNoInteractions(stockService);
    }

    @Test
    void inactiveOrCancelledFamilyCannotReceiveDistribution() {
        Family family = family(3L);
        family.setStatus(FamilyStatus.CANCELADA);
        when(distributionRepository.findByIdempotencyKey("request-9")).thenReturn(Optional.empty());
        when(familyRepository.findById(3L)).thenReturn(Optional.of(family));

        assertThatThrownBy(() -> service.create(request(3L, "request-9", item(8L, 1))))
            .isInstanceOf(ConflictException.class);
        verifyNoInteractions(stockService);
        verify(productRepository, never()).findAllByIdForUpdate(anyList());
    }

    @Test
    void duplicateIdempotencyKeyReturnsOriginalDistributionWithoutStockMovement() {
        Distribution existing = new Distribution();
        existing.setId(19L);
        existing.setFamily(family(3L));
        existing.setObservations("Entrega");
        existing.setReference("Comprovante");
        existing.setIdempotencyKey("request-9");
        existing.setCreatedBy("diacono-test");
        existing.setCreatedAt(LocalDateTime.now());
        when(distributionRepository.findByIdempotencyKey("request-9")).thenReturn(Optional.of(existing));
        DistributionItem item = new DistributionItem();
        item.setProduct(product(8L, "Arroz", 5));
        item.setQuantity(2);
        item.setUnit("pacote");
        when(itemRepository.findByDistributionIdOrderByIdAsc(19L)).thenReturn(List.of(item));

        var response = service.create(request(3L, "request-9", item(8L, 2)));

        assertThat(response.id()).isEqualTo(19L);
        verifyNoInteractions(stockService, productRepository);
        verify(distributionRepository, never()).save(any());
    }

    private CreateDistributionRequest request(Long familyId, String key, DistributionItemRequest... items) {
        CreateDistributionRequest request = new CreateDistributionRequest();
        request.setFamilyId(familyId);
        request.setItems(List.of(items));
        request.setObservation("Entrega");
        request.setReference("Comprovante");
        request.setIdempotencyKey(key);
        return request;
    }

    private DistributionItemRequest item(Long productId, int quantity) {
        DistributionItemRequest item = new DistributionItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private Family family(Long id) {
        Family family = new Family();
        family.setId(id);
        family.setResponsibleName("Família teste");
        family.setStatus(FamilyStatus.CADASTRADA);
        return family;
    }

    private Product product(Long id, String name, int balance) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setUnit("pacote");
        product.setCurrentQuantity(balance);
        product.setActive(true);
        return product;
    }
}
