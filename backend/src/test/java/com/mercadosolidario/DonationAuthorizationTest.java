package com.mercadosolidario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.dto.donation.*;
import com.mercadosolidario.model.enums.DonationStatus;
import com.mercadosolidario.service.DonationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DonationAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DonationService donationService;

    @Test
    void unauthenticatedUserCanCreatePendingFinancialDonation() throws Exception {
        when(donationService.createFinancial(any())).thenReturn(
            new PublicDonationResponse(1L, new BigDecimal("10.00"), DonationStatus.AGUARDANDO_PAGAMENTO,
                "PIX_NAO_CONFIGURADO", null, null, "Aguardando pagamento"));
        CreateFinancialDonationRequest request = new CreateFinancialDonationRequest();
        request.setAmount(new BigDecimal("10.00"));
        request.setAnonymous(true);
        request.setExternalReference("public-request");

        mockMvc.perform(post("/api/donations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
            .andExpect(jsonPath("$.pixStatus").value("PIX_NAO_CONFIGURADO"));
    }

    @Test
    void unauthenticatedUserCannotReadAdministrativeDonationsOrPixConfiguration() throws Exception {
        mockMvc.perform(get("/api/donations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/donations/configuration")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "diacono", roles = "DIACONO")
    void diaconoCannotReadOrManageDonationFinancialData() throws Exception {
        mockMvc.perform(get("/api/donations")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/donations/1/confirm-manually")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/donations/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void rhCanConsultButCannotManuallyConfirmOrConfigurePix() throws Exception {
        when(donationService.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/donations")).andExpect(status().isOk());
        mockMvc.perform(post("/api/donations/1/confirm-manually")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/donations/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }
}
