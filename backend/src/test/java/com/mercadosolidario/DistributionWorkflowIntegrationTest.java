package com.mercadosolidario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.dto.distribution.CreateDistributionRequest;
import com.mercadosolidario.dto.distribution.DistributionItemRequest;
import com.mercadosolidario.model.*;
import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.model.enums.RequestReason;
import com.mercadosolidario.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DistributionWorkflowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DistributionRepository distributionRepository;
    @Autowired private DistributionItemRepository itemRepository;
    @Autowired private StockMovementRepository movementRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Test
    @WithMockUser(username = "distributor", roles = "DIACONO")
    void multipleItemsDecreaseBalancesAtomicallyAndMarkFamilyContemplated() throws Exception {
        createUser("distributor", "DIACONO");
        Family family = createFamily();
        Product rice = createProduct("Arroz", 10);
        Product beans = createProduct("Feijão", 8);
        CreateDistributionRequest request = request(
            family.getId(), "multi-item-key",
            new Item(rice.getId(), 4), new Item(beans.getId(), 3));

        String response = mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMADA"))
            .andExpect(jsonPath("$.createdBy").value("distributor"))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andReturn().getResponse().getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        long distributionId = body.get("id").asLong();
        assertThat(productRepository.findById(rice.getId()).orElseThrow().getCurrentQuantity()).isEqualTo(6);
        assertThat(productRepository.findById(beans.getId()).orElseThrow().getCurrentQuantity()).isEqualTo(5);
        assertThat(familyRepository.findById(family.getId()).orElseThrow().getStatus())
            .isEqualTo(FamilyStatus.CONTEMPLADA);
        List<StockMovement> movements = movementRepository.findByFamilyId(family.getId());
        assertThat(movements).hasSize(2).allSatisfy(movement -> {
            assertThat(movement.getType().name()).isEqualTo("SAIDA_DISTRIBUICAO");
            assertThat(movement.getUser().getUsername()).isEqualTo("distributor");
            assertThat(movement.getPreviousBalance()).isNotNull();
            assertThat(movement.getNewBalance()).isNotNull();
        });
        assertThat(itemRepository.findByDistributionIdOrderByIdAsc(distributionId))
            .hasSize(2).allSatisfy(item -> assertThat(item.getStockMovement()).isNotNull());
        assertThat(auditLogRepository.findByEntityTypeAndEntityId("Distribution", distributionId))
            .anySatisfy(audit -> assertThat(audit.getAction()).isEqualTo("DISTRIBUTION_CONFIRMED"));
    }

    @Test
    @WithMockUser(username = "distributor", roles = "RH_TESOURARIA")
    void insufficientSecondItemDoesNotPersistAnyDistributionOrStockOut() throws Exception {
        createUser("distributor", "RH_TESOURARIA");
        Family family = createFamily();
        Product enough = createProduct("Produto disponível", 10);
        Product scarce = createProduct("Produto insuficiente", 1);
        CreateDistributionRequest request = request(
            family.getId(), "insufficient-key",
            new Item(enough.getId(), 2), new Item(scarce.getId(), 5));

        mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Estoque insuficiente")));

        assertThat(distributionRepository.findByIdempotencyKey("insufficient-key")).isEmpty();
        assertThat(productRepository.findById(enough.getId()).orElseThrow().getCurrentQuantity()).isEqualTo(10);
        assertThat(productRepository.findById(scarce.getId()).orElseThrow().getCurrentQuantity()).isEqualTo(1);
        assertThat(movementRepository.findByFamilyId(family.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "distributor", roles = "DIACONO")
    void repeatedIdempotencyKeyDoesNotDuplicateStockMovement() throws Exception {
        createUser("distributor", "DIACONO");
        Family family = createFamily();
        Product product = createProduct("Produto idempotente", 9);
        CreateDistributionRequest request = request(family.getId(), "repeat-key", new Item(product.getId(), 3));
        String json = objectMapper.writeValueAsString(request);

        String first = mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(second).get("id").asLong())
            .isEqualTo(objectMapper.readTree(first).get("id").asLong());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentQuantity()).isEqualTo(6);
        assertThat(movementRepository.findByFamilyId(family.getId())).hasSize(1);
        assertThat(distributionRepository.findAllByOrderByCreatedAtDesc()).hasSize(1);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cancelDoesNotDeleteHistoryOrRestoreStockAutomatically() throws Exception {
        createUser("admin", "ADMIN");
        Family family = createFamily();
        Product product = createProduct("Produto cancelamento", 7);
        CreateDistributionRequest request = request(family.getId(), "cancel-key", new Item(product.getId(), 2));
        String json = objectMapper.writeValueAsString(request);
        String created = mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long distributionId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/api/distributions/" + distributionId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Revisão operacional\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELADA"));

        assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentQuantity()).isEqualTo(5);
        assertThat(itemRepository.findByDistributionIdOrderByIdAsc(distributionId)).hasSize(1);
        assertThat(movementRepository.findByFamilyId(family.getId())).hasSize(1);
        assertThat(familyRepository.findById(family.getId()).orElseThrow().getStatus())
            .isEqualTo(FamilyStatus.APROVADA);
    }

    @Test
    @WithMockUser(username = "distributor", roles = "DIACONO")
    void contemplatedFamilyCannotReceiveSecondDistribution() throws Exception {
        createUser("distributor", "DIACONO");
        Family family = createFamily();
        Product product = createProduct("Produto segunda cesta", 9);
        String first = objectMapper.writeValueAsString(
            request(family.getId(), "first-basket", new Item(product.getId(), 2)));
        mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON).content(first))
            .andExpect(status().isOk());
        assertThat(familyRepository.findById(family.getId()).orElseThrow().getStatus())
            .isEqualTo(FamilyStatus.CONTEMPLADA);

        String second = objectMapper.writeValueAsString(
            request(family.getId(), "second-basket", new Item(product.getId(), 1)));
        mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON).content(second))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("já foi contemplada")));
        assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentQuantity()).isEqualTo(7);
    }

    @Test
    @WithMockUser(username = "distributor", roles = "DIACONO")
    void nonexistentFamilyReturnsNotFoundAndInvalidQuantitiesReturnBadRequest() throws Exception {
        createUser("distributor", "DIACONO");
        Product product = createProduct("Produto validação", 7);
        mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request(999999L, "missing-family", new Item(product.getId(), 1)))))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request(999999L, "zero-quantity", new Item(product.getId(), 0)))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "distributor", roles = "DIACONO")
    void nonexistentProductReturnsNotFound() throws Exception {
        createUser("distributor", "DIACONO");
        Family family = createFamily();

        mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request(
                    family.getId(), "missing-product", new Item(999999L, 1)))))
            .andExpect(status().isNotFound());

        assertThat(distributionRepository.findByIdempotencyKey("missing-product")).isEmpty();
    }

    @Test
    @WithMockUser(username = "distributor", roles = "DIACONO")
    void sameKeyWithDifferentPayloadReturnsConflict() throws Exception {
        createUser("distributor", "DIACONO");
        Family family = createFamily();
        Product product = createProduct("Produto conflitante", 5);
        CreateDistributionRequest first = request(family.getId(), "key-conflict", new Item(product.getId(), 1));
        String json = objectMapper.writeValueAsString(first);

        mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk());
        first.setItems(List.of(itemRequest(product.getId(), 2)));
        mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)))
            .andExpect(status().isConflict());

        assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentQuantity()).isEqualTo(4);
        assertThat(movementRepository.findByFamilyId(family.getId())).hasSize(1);
    }

    private void createUser(String username, String roleName) {
        if (userRepository.findByUsername(username).isPresent()) return;
        Role role = roleRepository.findByName(roleName).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(roleName);
            newRole.setDescription(roleName);
            return roleRepository.save(newRole);
        });
        User user = new User();
        user.setUsername(username);
        user.setPassword("test-only-hash");
        user.setEmail(username + "@example.test");
        user.setFullName("Usuário de teste");
        user.getRoles().add(role);
        userRepository.save(user);
    }

    private Family createFamily() {
        Family family = new Family();
        family.setResponsibleName("Família de distribuição");
        family.setCpf("52998224725");
        family.setHouseholdSize(4);
        family.setTotalIncome(new BigDecimal("1200.00"));
        family.setRequestReason(RequestReason.BAIXA_RENDA);
        family.setOrigin(FamilyOrigin.CADASTRO_PROPRIO);
        family.setStatus(FamilyStatus.APROVADA);
        return familyRepository.save(family);
    }

    private Product createProduct(String name, int quantity) {
        Product product = new Product();
        product.setName(name);
        product.setUnit("unidade");
        product.setCurrentQuantity(quantity);
        product.setMinimumQuantity(1);
        product.setActive(true);
        return productRepository.save(product);
    }

    private CreateDistributionRequest request(Long familyId, String key, Item... items) {
        CreateDistributionRequest request = new CreateDistributionRequest();
        request.setFamilyId(familyId);
        request.setIdempotencyKey(key);
        request.setObservation("Entrega de teste");
        request.setItems(java.util.Arrays.stream(items).map(value -> {
            DistributionItemRequest item = new DistributionItemRequest();
            item.setProductId(value.productId());
            item.setQuantity(value.quantity());
            return item;
        }).toList());
        return request;
    }

    private DistributionItemRequest itemRequest(Long productId, int quantity) {
        DistributionItemRequest item = new DistributionItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private record Item(Long productId, int quantity) {}
}
