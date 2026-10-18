package com.mercadosolidario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.dto.family.FamilyCreateRequest;
import com.mercadosolidario.dto.family.FamilyMemberRequest;
import com.mercadosolidario.dto.family.FamilyResponse;
import com.mercadosolidario.dto.family.FamilyUpdateRequest;
import com.mercadosolidario.model.Family;
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
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FamilyControllerTest {

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

    private User testUser;
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

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setActive(true);
        testUser.getRoles().add(adminRole);
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testCreateFamily() throws Exception {
        FamilyCreateRequest request = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725") // Valid CPF for testing
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.responsibleName").value("João Silva"))
                .andExpect(jsonPath("$.cpf").value("529.982.247-25"))
                .andExpect(jsonPath("$.status").value("CADASTRADA"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testCreateFamilyWithInvalidCpf() throws Exception {
        FamilyCreateRequest request = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("11111111111") // Invalid CPF (all same digits)
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testCreateFamilyWithDuplicateCpf() throws Exception {
        // Create first family
        FamilyCreateRequest request1 = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725") // Valid CPF
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Try to create second family with same CPF
        FamilyCreateRequest request2 = FamilyCreateRequest.builder()
            .responsibleName("Maria Santos")
            .cpf("52998224725") // Same CPF
            .householdSize(3)
            .totalIncome(new BigDecimal("1200.00"))
            .requestReason(RequestReason.DESEMPREGO)
            .origin(FamilyOrigin.INDICACAO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testGetFamilyById() throws Exception {
        // Create family first
        FamilyCreateRequest createRequest = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725") // Valid CPF
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(familyId))
                .andExpect(jsonPath("$.responsibleName").value("João Silva"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testUpdateFamily() throws Exception {
        // Create family first
        FamilyCreateRequest createRequest = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725") // Valid CPF
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

        // Update family
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsibleName").value("João Silva Santos"))
                .andExpect(jsonPath("$.householdSize").value(5))
                .andExpect(jsonPath("$.totalIncome").value(1800.00));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testUpdateFamilyStatus() throws Exception {
        // Create family first
        FamilyCreateRequest createRequest = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725") // Valid CPF
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANALISE"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testSearchFamilies() throws Exception {
        // Create families
        FamilyCreateRequest request1 = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725") // Valid CPF
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        FamilyCreateRequest request2 = FamilyCreateRequest.builder()
            .responsibleName("Maria Santos")
            .cpf("12345678909") // Valid CPF
            .householdSize(3)
            .totalIncome(new BigDecimal("1200.00"))
            .requestReason(RequestReason.DESEMPREGO)
            .origin(FamilyOrigin.INDICACAO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

        // Search all
        mockMvc.perform(get("/api/families"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // Business validation tests

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testCreateFamilyWithNegativeIncome() throws Exception {
        FamilyCreateRequest request = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725")
            .householdSize(4)
            .totalIncome(new BigDecimal("-100.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testCreateFamilyWithZeroHouseholdSize() throws Exception {
        FamilyCreateRequest request = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725")
            .householdSize(0)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testCreateFamilyWithEmptyName() throws Exception {
        FamilyCreateRequest request = FamilyCreateRequest.builder()
            .responsibleName("")
            .cpf("52998224725")
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testGetNonExistentFamily() throws Exception {
        mockMvc.perform(get("/api/families/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testUpdateNonExistentFamily() throws Exception {
        FamilyUpdateRequest updateRequest = FamilyUpdateRequest.builder()
            .responsibleName("João Silva")
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(put("/api/families/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testUpdateStatusWithInvalidStatus() throws Exception {
        // Create family first
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

        // Try to update with invalid status
        mockMvc.perform(patch("/api/families/" + familyId + "/status")
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    // Status transition tests

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testValidStatusTransitionCadastroToEmAnalise() throws Exception {
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

        mockMvc.perform(patch("/api/families/" + familyId + "/status")
                        .param("status", "EM_ANALISE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANALISE"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testValidStatusTransitionEmAnaliseToAprovada() throws Exception {
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

        // First transition
        mockMvc.perform(patch("/api/families/" + familyId + "/status")
                        .param("status", "EM_ANALISE"))
                .andExpect(status().isOk());

        // Second transition
        mockMvc.perform(patch("/api/families/" + familyId + "/status")
                        .param("status", "APROVADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testInvalidStatusTransitionAtendidaToCadastrada() throws Exception {
        // Create family directly and set to ATENDIDA
        com.mercadosolidario.model.Family family = new com.mercadosolidario.model.Family();
        family.setResponsibleName("João Silva");
        family.setCpf("52998224725");
        family.setHouseholdSize(4);
        family.setTotalIncome(new BigDecimal("1500.00"));
        family.setRequestReason(RequestReason.BAIXA_RENDA);
        family.setOrigin(FamilyOrigin.CADASTRO_PROPRIO);
        family.setStatus(FamilyStatus.ATENDIDA);
        family = familyRepository.save(family);

        // Try to transition back to CADASTRADA
        mockMvc.perform(patch("/api/families/" + family.getId() + "/status")
                        .param("status", "CADASTRADA"))
                .andExpect(status().isBadRequest());
    }

    // Filter and pagination tests

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testSearchFamiliesByName() throws Exception {
        FamilyCreateRequest request1 = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725")
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        FamilyCreateRequest request2 = FamilyCreateRequest.builder()
            .responsibleName("Maria Santos")
            .cpf("12345678909")
            .householdSize(3)
            .totalIncome(new BigDecimal("1200.00"))
            .requestReason(RequestReason.DESEMPREGO)
            .origin(FamilyOrigin.INDICACAO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

        // Search by name
        mockMvc.perform(get("/api/families")
                        .param("name", "João"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testSearchFamiliesByStatus() throws Exception {
        FamilyCreateRequest request1 = FamilyCreateRequest.builder()
            .responsibleName("João Silva")
            .cpf("52998224725")
            .householdSize(4)
            .totalIncome(new BigDecimal("1500.00"))
            .requestReason(RequestReason.BAIXA_RENDA)
            .origin(FamilyOrigin.CADASTRO_PROPRIO)
            .build();

        mockMvc.perform(post("/api/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Search by status
        mockMvc.perform(get("/api/families")
                        .param("status", "CADASTRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testPagination() throws Exception {
        // Create 3 families with valid CPFs
        String[] validCpfs = {"52998224725", "12345678909", "98765432100"};
        for (int i = 0; i < 3; i++) {
            FamilyCreateRequest request = FamilyCreateRequest.builder()
                .responsibleName("Test User " + i)
                .cpf(validCpfs[i])
                .householdSize(2)
                .totalIncome(new BigDecimal("1000.00"))
                .requestReason(RequestReason.BAIXA_RENDA)
                .origin(FamilyOrigin.CADASTRO_PROPRIO)
                .build();

            mockMvc.perform(post("/api/families")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // Test page 0 with size 2
        mockMvc.perform(get("/api/families")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testEmptyList() throws Exception {
        mockMvc.perform(get("/api/families"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
