package com.mercadosolidario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.dto.distribution.CreateDistributionRequest;
import com.mercadosolidario.dto.distribution.DistributionItemRequest;
import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.model.Role;
import com.mercadosolidario.model.User;
import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.model.enums.RequestReason;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.ProductRepository;
import com.mercadosolidario.repository.RoleRepository;
import com.mercadosolidario.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DistributionAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    @Test
    void anonymousCannotCreateOrReadDistributions() throws Exception {
        mockMvc.perform(get("/api/distributions")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "distributor", roles = "DIACONO")
    void diaconoCanRegisterDistributionButCannotCancelIt() throws Exception {
        createUser("distributor");
        Family family = createFamily();
        Product product = createProduct();
        mockMvc.perform(post("/api/distributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request(family.getId(), product.getId(), "diacono-key"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMADA"));
        mockMvc.perform(post("/api/distributions/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Revisão\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "USER")
    void authenticatedUserWithoutOperationalRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/distributions")).andExpect(status().isForbidden());
    }

    private void createUser(String username) {
        if (userRepository.findByUsername(username).isPresent()) return;
        Role role = roleRepository.findByName("DIACONO").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("DIACONO");
            newRole.setDescription("Diácono");
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
        family.setResponsibleName("Família para teste");
        family.setCpf("52998224725");
        family.setHouseholdSize(2);
        family.setTotalIncome(new BigDecimal("1000.00"));
        family.setRequestReason(RequestReason.BAIXA_RENDA);
        family.setOrigin(FamilyOrigin.CADASTRO_PROPRIO);
        family.setStatus(FamilyStatus.CADASTRADA);
        return familyRepository.save(family);
    }

    private Product createProduct() {
        Product product = new Product();
        product.setName("Produto para teste");
        product.setUnit("unidade");
        product.setCurrentQuantity(5);
        product.setMinimumQuantity(1);
        product.setActive(true);
        return productRepository.save(product);
    }

    private CreateDistributionRequest request(Long familyId, Long productId, String key) {
        DistributionItemRequest item = new DistributionItemRequest();
        item.setProductId(productId);
        item.setQuantity(1);
        CreateDistributionRequest request = new CreateDistributionRequest();
        request.setFamilyId(familyId);
        request.setItems(java.util.List.of(item));
        request.setIdempotencyKey(key);
        return request;
    }
}
