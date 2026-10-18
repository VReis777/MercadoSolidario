package com.mercadosolidario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.dto.donation.CreateProductDonationRequest;
import com.mercadosolidario.model.Donation;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.model.enums.DonationStatus;
import com.mercadosolidario.model.enums.DonationType;
import com.mercadosolidario.repository.DonationRepository;
import com.mercadosolidario.repository.ProductRepository;
import com.mercadosolidario.repository.StockMovementRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DonationWorkflowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductRepository productRepository;
    @Autowired private DonationRepository donationRepository;
    @Autowired private StockMovementRepository movementRepository;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void repeatedProductDonationRequestUpdatesOfficialStockOnlyOnce() throws Exception {
        Product product = product("Feijão");
        CreateProductDonationRequest request = new CreateProductDonationRequest();
        request.setProductId(product.getId());
        request.setQuantity(4);
        request.setDonorName("Doador de teste");
        request.setExternalReference("integration-idempotency-key");
        String body = objectMapper.writeValueAsString(request);

        String first = mockMvc.perform(post("/api/donations/products")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("PRODUTO"))
            .andExpect(jsonPath("$.status").value("CONFIRMADA_MANUALMENTE"))
            .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/donations/products")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode firstResult = objectMapper.readTree(first);
        JsonNode secondResult = objectMapper.readTree(second);
        assertThat(secondResult.get("id").asLong()).isEqualTo(firstResult.get("id").asLong());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentQuantity()).isEqualTo(4);
        assertThat(movementRepository.findByProductIdOrderByCreatedAtDesc(product.getId())).hasSize(1);
        assertThat(donationRepository.findByExternalReference("integration-idempotency-key"))
            .get().satisfies(donation -> {
                assertThat(donation.getType()).isEqualTo(DonationType.PRODUTO);
                assertThat(donation.getStatus()).isEqualTo(DonationStatus.CONFIRMADA_MANUALMENTE);
            });
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void nonexistentProductIsNotFoundAndDoesNotCreateDonation() throws Exception {
        CreateProductDonationRequest request = new CreateProductDonationRequest();
        request.setProductId(999999L);
        request.setQuantity(2);
        request.setExternalReference("missing-product-reference");

        mockMvc.perform(post("/api/donations/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        assertThat(donationRepository.findByExternalReference("missing-product-reference")).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void manuallyConfirmedDonationCannotBeConfirmedAgain() throws Exception {
        Donation donation = new Donation();
        donation.setType(DonationType.FINANCEIRA);
        donation.setAmount(new BigDecimal("30.00"));
        donation.setStatus(DonationStatus.AGUARDANDO_PAGAMENTO);
        donation.setExternalReference("manual-confirmation-reference");
        donation.setIsAnonymous(true);
        Donation saved = donationRepository.save(donation);

        mockMvc.perform(post("/api/donations/" + saved.getId() + "/confirm-manually"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMADA_MANUALMENTE"))
            .andExpect(jsonPath("$.confirmedBy").value("admin"));
        mockMvc.perform(post("/api/donations/" + saved.getId() + "/confirm-manually"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void financialDonationRejectsZeroAndExcessPrecision() throws Exception {
        mockMvc.perform(post("/api/donations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amount":0,"anonymous":true,"externalReference":"invalid-zero"}
                    """))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/donations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amount":1.001,"anonymous":true,"externalReference":"invalid-precision"}
                    """))
            .andExpect(status().isBadRequest());
    }

    private Product product(String name) {
        Product product = new Product();
        product.setName(name);
        product.setCategory("Alimentos");
        product.setUnit("pacote");
        product.setCurrentQuantity(0);
        product.setMinimumQuantity(1);
        product.setActive(true);
        return productRepository.save(product);
    }
}
