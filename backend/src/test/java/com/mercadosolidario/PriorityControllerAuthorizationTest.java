package com.mercadosolidario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mercadosolidario.dto.priority.PriorityConfigurationRequest;
import com.mercadosolidario.dto.priority.PriorityFactorRequest;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.PriorityConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PriorityControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PriorityConfigurationRepository configurationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanConfigurations() {
        configurationRepository.deleteAll();
    }

    @Test
    void anonymousCannotReadConfiguration() throws Exception {
        mockMvc.perform(get("/api/priority/configuration"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "diacono", roles = "DIACONO")
    void diaconoCannotChangeConfiguration() throws Exception {
        mockMvc.perform(post("/api/priority/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void rhCanReadPriorityButCannotManageConfiguration() throws Exception {
        mockMvc.perform(get("/api/families/priority"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PRIORIDADE_NAO_CONFIGURADA"));

        mockMvc.perform(get("/api/priority/configuration"))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReadFamilyPriority() throws Exception {
        mockMvc.perform(get("/api/families/priority"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "diacono", roles = "DIACONO")
    void diaconoCannotReadPriorityResultsUntilPolicyIsApproved() throws Exception {
        mockMvc.perform(get("/api/families/priority"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void missingFamilyPriorityReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/families/999999/priority"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCreatesConfigurationInactiveByDefault() throws Exception {
        mockMvc.perform(post("/api/priority/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.factors[0].enabled").value(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void editingConfigurationCreatesAnAuditedInactiveVersion() throws Exception {
        String response = mockMvc.perform(post("/api/priority/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        long id = created.get("id").asLong();

        String versionResponse = mockMvc.perform(put("/api/priority/configuration/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.active").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();
        long versionId = objectMapper.readTree(versionResponse).get("id").asLong();

        assertThat(configurationRepository.findAllByOrderByCodeAscVersionNumberDesc())
            .hasSize(2)
            .allSatisfy(configuration -> assertThat(configuration.isActive()).isFalse());
        assertThat(auditLogRepository.findByEntityTypeAndEntityId("PriorityConfiguration", id))
            .hasSize(1);
        assertThat(auditLogRepository.findByEntityTypeAndEntityId("PriorityConfiguration", versionId))
            .hasSize(1);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void activationIsExplicitAndRequiresAnEnabledFactor() throws Exception {
        PriorityConfigurationRequest disabled = validRequest();
        String response = mockMvc.perform(post("/api/priority/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(disabled)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/priority/configuration/" + id + "/activate"))
            .andExpect(status().isConflict());

        disabled.getFactors().getFirst().setEnabled(true);
        String newVersion = mockMvc.perform(put("/api/priority/configuration/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(disabled)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();
        long newVersionId = objectMapper.readTree(newVersion).get("id").asLong();
        mockMvc.perform(post("/api/priority/configuration/" + newVersionId + "/activate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));
    }

    private PriorityConfigurationRequest validRequest() {
        PriorityFactorRequest factor = new PriorityFactorRequest();
        factor.setFactorCode("HOUSEHOLD_SIZE");
        factor.setLabel("Pessoas no domicílio — demonstração técnica");
        factor.setOperator("GTE");
        factor.setReferenceValue("5");
        factor.setWeight(new BigDecimal("3.00"));
        factor.setExplanation("CONFIGURAÇÃO DE DEMONSTRAÇÃO — NÃO É REGRA SOCIAL OFICIAL");

        PriorityConfigurationRequest request = new PriorityConfigurationRequest();
        request.setCode("DEMO-RULES");
        request.setName("Configuração de demonstração técnica");
        request.setExplanation("CONFIGURAÇÃO DE DEMONSTRAÇÃO — NÃO É REGRA SOCIAL OFICIAL");
        request.setFactors(List.of(factor));
        return request;
    }
}
