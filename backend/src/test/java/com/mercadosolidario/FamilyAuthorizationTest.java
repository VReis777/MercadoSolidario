package com.mercadosolidario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.dto.family.FamilyCreateRequest;
import com.mercadosolidario.dto.family.FamilyResponse;
import com.mercadosolidario.dto.family.FamilyUpdateRequest;
import com.mercadosolidario.model.Role;
import com.mercadosolidario.model.User;
import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.model.enums.RequestReason;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.RoleRepository;
import com.mercadosolidario.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class FamilyAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role adminRole;
    private Role rhRole;
    private Role diacanoRole;

    @BeforeEach
    void setUp() {
        // Create roles
        adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator");
        adminRole = roleRepository.save(adminRole);

        rhRole = new Role();
        rhRole.setName("RH_TESOURARIA");
        rhRole.setDescription("RH/Tesouraria");
        rhRole = roleRepository.save(rhRole);

        diacanoRole = new Role();
        diacanoRole.setName("DIACONO");
        diacanoRole.setDescription("Diácono");
        diacanoRole = roleRepository.save(diacanoRole);
    }

    // CREATE tests

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminCanCreateFamily() throws Exception {
        FamilyCreateRequest request = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725")
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void testRhCanCreateFamily() throws Exception {
        FamilyCreateRequest request = FamilyCreateRequest.builder()
            .responsibleName("Maria Santos")
            .cpf("12345678909")
            .householdSize(3)
            .totalIncome(new BigDecimal("1200.00"))
            .requestReason(RequestReason.DESEMPREGO)
            .origin(FamilyOrigin.INDICACAO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "diakono", roles = "DIACONO")
    void testDiakonoCannotCreateFamily() throws Exception {
        FamilyCreateRequest request = FamilyCreateRequest.builder()
            .responsibleName("Pedro Costa")
            .cpf("98765432100")
            .householdSize(2)
            .totalIncome(new BigDecimal("1000.00"))
            .requestReason(RequestReason.EMERGENCIA_FINANCEIRA)
            .origin(FamilyOrigin.DIACONO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // READ tests

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminCanReadFamily() throws Exception {
        // Create family
        FamilyCreateRequest createRequest = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725")
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        String response = mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        FamilyResponse familyResponse = objectMapper.readValue(response, FamilyResponse.class);
        Long familyId = familyResponse.getId();

        mockMvc.perform(get("/api/families/" + familyId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void testRhCanReadFamily() throws Exception {
        // Create family
        FamilyCreateRequest createRequest = FamilyCreateRequest.builder()
            .responsibleName("Maria Santos")
            .cpf("12345678909")
            .householdSize(3)
            .totalIncome(new BigDecimal("1200.00"))
            .requestReason(RequestReason.DESEMPREGO)
            .origin(FamilyOrigin.INDICACAO)
            .build();

        String response = mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        FamilyResponse familyResponse = objectMapper.readValue(response, FamilyResponse.class);
        Long familyId = familyResponse.getId();

        mockMvc.perform(get("/api/families/" + familyId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "diakono", roles = "DIACONO")
    void testDiakonoCanReadFamily() throws Exception {
        // Create family first (as admin in a separate call)
        // For this test, we'll create the family directly in the repository
        com.mercadosolidario.model.Family family = new com.mercadosolidario.model.Family();
        family.setResponsibleName("Pedro Costa");
        family.setCpf("98765432100");
        family.setHouseholdSize(2);
        family.setTotalIncome(new BigDecimal("1000.00"));
        family.setRequestReason(RequestReason.EMERGENCIA_FINANCEIRA);
        family.setOrigin(FamilyOrigin.DIACONO);
        family.setStatus(FamilyStatus.CADASTRADA);
        family = familyRepository.save(family);

        mockMvc.perform(get("/api/families/" + family.getId()))
                .andExpect(status().isOk());
    }

    // UPDATE tests

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminCanUpdateFamily() throws Exception {
        // Create family
        FamilyCreateRequest createRequest = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725")
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        String response = mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        FamilyResponse familyResponse = objectMapper.readValue(response, FamilyResponse.class);
        Long familyId = familyResponse.getId();

        // Update
        FamilyUpdateRequest updateRequest = FamilyUpdateRequest.builder()
            .responsibleName("João Silva Santos")
            .householdSize(5)
            .totalIncome(new BigDecimal("1800.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(put("/api/families/" + familyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void testRhCanUpdateFamily() throws Exception {
        // Create family
        FamilyCreateRequest createRequest = FamilyCreateRequest.builder()
            .responsibleName("Maria Santos")
            .cpf("12345678909")
            .householdSize(3)
            .totalIncome(new BigDecimal("1200.00"))
            .requestReason(RequestReason.DESEMPREGO)
            .origin(FamilyOrigin.INDICACAO)
            .build();

        String response = mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        FamilyResponse familyResponse = objectMapper.readValue(response, FamilyResponse.class);
        Long familyId = familyResponse.getId();

        // Update
        FamilyUpdateRequest updateRequest = FamilyUpdateRequest.builder()
            .responsibleName("Maria Santos Silva")
            .householdSize(4)
            .totalIncome(new BigDecimal("1400.00"))
            .requestReason(RequestReason.DESEMPREGO)
            .origin(FamilyOrigin.INDICACAO)
            .build();

        mockMvc.perform(put("/api/families/" + familyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "diakono", roles = "DIACONO")
    void testDiakonoCannotUpdateFamily() throws Exception {
        // Create family directly
        com.mercadosolidario.model.Family family = new com.mercadosolidario.model.Family();
        family.setResponsibleName("Pedro Costa");
        family.setCpf("98765432100");
        family.setHouseholdSize(2);
        family.setTotalIncome(new BigDecimal("1000.00"));
        family.setRequestReason(RequestReason.EMERGENCIA_FINANCEIRA);
        family.setOrigin(FamilyOrigin.DIACONO);
        family.setStatus(FamilyStatus.CADASTRADA);
        family = familyRepository.save(family);

        // Try to update
        FamilyUpdateRequest updateRequest = FamilyUpdateRequest.builder()
            .responsibleName("Pedro Costa Silva")
            .householdSize(3)
            .totalIncome(new BigDecimal("1200.00"))
            .requestReason(RequestReason.EMERGENCIA_FINANCEIRA)
            .origin(FamilyOrigin.DIACONO)
            .build();

        mockMvc.perform(put("/api/families/" + family.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    // STATUS UPDATE tests

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminCanUpdateStatus() throws Exception {
        // Create family
        FamilyCreateRequest createRequest = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725")
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        String response = mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        FamilyResponse familyResponse = objectMapper.readValue(response, FamilyResponse.class);
        Long familyId = familyResponse.getId();

        // Update status
        mockMvc.perform(patch("/api/families/" + familyId + "/status")
                        .param("status", "EM_ANALISE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void testRhCanUpdateStatus() throws Exception {
        // Create family
        FamilyCreateRequest createRequest = FamilyCreateRequest.builder()
            .responsibleName("Maria Santos")
            .cpf("12345678909")
            .householdSize(3)
            .totalIncome(new BigDecimal("1200.00"))
            .requestReason(RequestReason.DESEMPREGO)
            .origin(FamilyOrigin.INDICACAO)
            .build();

        String response = mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        FamilyResponse familyResponse = objectMapper.readValue(response, FamilyResponse.class);
        Long familyId = familyResponse.getId();

        // Update status
        mockMvc.perform(patch("/api/families/" + familyId + "/status")
                        .param("status", "EM_ANALISE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "diakono", roles = "DIACONO")
    void testDiakonoCannotUpdateStatus() throws Exception {
        // Create family directly
        com.mercadosolidario.model.Family family = new com.mercadosolidario.model.Family();
        family.setResponsibleName("Pedro Costa");
        family.setCpf("98765432100");
        family.setHouseholdSize(2);
        family.setTotalIncome(new BigDecimal("1000.00"));
        family.setRequestReason(RequestReason.EMERGENCIA_FINANCEIRA);
        family.setOrigin(FamilyOrigin.DIACONO);
        family.setStatus(FamilyStatus.CADASTRADA);
        family = familyRepository.save(family);

        // Try to update status
        mockMvc.perform(patch("/api/families/" + family.getId() + "/status")
                        .param("status", "EM_ANALISE"))
                .andExpect(status().isForbidden());
    }

    // Unauthenticated tests

    @Test
    void testUnauthenticatedCannotCreateFamily() throws Exception {
        FamilyCreateRequest request = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725")
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUnauthenticatedCannotReadFamily() throws Exception {
        mockMvc.perform(get("/api/families/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUnauthenticatedCannotUpdateFamily() throws Exception {
        FamilyUpdateRequest updateRequest = FamilyUpdateRequest.builder()
            .responsibleName("João Silva")
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(put("/api/families/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUnauthenticatedCannotUpdateStatus() throws Exception {
        mockMvc.perform(patch("/api/families/1/status")
                        .param("status", "EM_ANALISE"))
                .andExpect(status().isUnauthorized());
    }
}
