package com.mercadosolidario;

import com.mercadosolidario.repository.DonationRepository;
import com.mercadosolidario.repository.DistributionRepository;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.ProductRepository;
import com.mercadosolidario.repository.StockMovementRepository;
import com.mercadosolidario.model.Donation;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.model.StockMovement;
import com.mercadosolidario.model.enums.DonationStatus;
import com.mercadosolidario.model.enums.DonationType;
import com.mercadosolidario.model.enums.MovementType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DistributionRepository distributionRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    void anonymousCannotReadDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "other", roles = "SPONSOR")
    void unauthorizedRoleCannotReadDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanReadAggregatedDashboardAndPriorityRemainsUnconfigured() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                .param("from", "2026-01-01")
                .param("to", "2026-01-30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.from").value("2026-01-01"))
            .andExpect(jsonPath("$.to").value("2026-01-30"))
            .andExpect(jsonPath("$.families.total").value(0))
            .andExpect(jsonPath("$.stock.activeProducts").value(0))
            .andExpect(jsonPath("$.donations.confirmedFinancialAmount").value(0))
            .andExpect(jsonPath("$.priorityConfigured").value(false));
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void rhCanReadDonationAndFinancialAggregates() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.donations").isNotEmpty())
            .andExpect(jsonPath("$.donations.confirmedFinancialAmount").value(0));
    }

    @Test
    @WithMockUser(username = "diacono", roles = "DIACONO")
    void diaconoReceivesOperationalMetricsButNoDonationData() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.families").isNotEmpty())
            .andExpect(jsonPath("$.stock").isNotEmpty())
            .andExpect(jsonPath("$.distributions").isNotEmpty())
            .andExpect(jsonPath("$.donations").doesNotExist())
            .andExpect(jsonPath("$..cpf").doesNotExist())
            .andExpect(jsonPath("$..pixKey").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rejectsInvalidAndExcessiveDateRanges() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").param("from", "2026-02-01").param("to", "2026-01-01"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/dashboard/summary").param("from", "2025-01-01").param("to", "2026-12-31"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/dashboard/summary").param("from", "not-a-date").param("to", "2026-01-01"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void readingDashboardDoesNotChangeBusinessRecords() throws Exception {
        long families = familyRepository.count();
        long products = productRepository.count();
        long donations = donationRepository.count();
        long distributions = distributionRepository.count();

        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk());

        assertThat(familyRepository.count()).isEqualTo(families);
        assertThat(productRepository.count()).isEqualTo(products);
        assertThat(donationRepository.count()).isEqualTo(donations);
        assertThat(distributionRepository.count()).isEqualTo(distributions);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void aggregatesPersistedStockAndOnlyOfficiallyReceivedFinancialDonations() throws Exception {
        Product product = new Product();
        product.setName("Arroz");
        product.setUnit("kg");
        product.setCurrentQuantity(4);
        product.setMinimumQuantity(5);
        product.setActive(true);
        productRepository.saveAndFlush(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setType(MovementType.ENTRADA_COMPRA);
        movement.setQuantity(4);
        movement.setPreviousBalance(0);
        movement.setNewBalance(4);
        movement.setReason("Compra recebida");
        stockMovementRepository.saveAndFlush(movement);

        Donation awaiting = donation(DonationType.FINANCEIRA, DonationStatus.AGUARDANDO_PAGAMENTO, "100.00");
        Donation received = donation(DonationType.FINANCEIRA, DonationStatus.PAGA, "25.00");
        Donation productReceived = donation(DonationType.PRODUTO, DonationStatus.CONFIRMADA_MANUALMENTE, null);
        donationRepository.saveAllAndFlush(java.util.List.of(awaiting, received, productReceived));

        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stock.activeProducts").value(1))
            .andExpect(jsonPath("$.stock.lowStockProducts").value(1))
            .andExpect(jsonPath("$.stock.lowStock[0].quantity").value(4))
            .andExpect(jsonPath("$.stock.lowStock[0].unit").value("kg"))
            .andExpect(jsonPath("$.stock.movementsByType.ENTRADA_COMPRA").value(1))
            .andExpect(jsonPath("$.donations.created").value(3))
            .andExpect(jsonPath("$.donations.awaitingPayment").value(1))
            .andExpect(jsonPath("$.donations.manuallyConfirmed").value(1))
            .andExpect(jsonPath("$.donations.productDonations").value(1))
            .andExpect(jsonPath("$.donations.confirmedFinancialAmount").value(25.00));
    }

    private Donation donation(DonationType type, DonationStatus status, String amount) {
        Donation donation = new Donation();
        donation.setType(type);
        donation.setStatus(status);
        if (amount != null) donation.setAmount(new java.math.BigDecimal(amount));
        return donation;
    }
}
