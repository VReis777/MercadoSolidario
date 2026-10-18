package com.mercadosolidario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.dto.family.FamilyMemberRequest;
import com.mercadosolidario.dto.family.FamilyMemberResponse;
import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.FamilyMember;
import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.model.enums.RequestReason;
import com.mercadosolidario.repository.FamilyMemberRepository;
import com.mercadosolidario.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class FamilyMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    private Long familyId;

    @BeforeEach
    void setup() {
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();

        Family family = new Family();
        family.setResponsibleName("João Silva");
        family.setCpf("52998224725");
        family.setHouseholdSize(2);
        family.setTotalIncome(new BigDecimal("1000.00"));
        family.setRequestReason(RequestReason.BAIXA_RENDA);
        family.setOrigin(FamilyOrigin.CADASTRO_PROPRIO);
        family.setStatus(FamilyStatus.CADASTRADA);
        family = familyRepository.save(family);
        familyId = family.getId();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testAddMember() throws Exception {
        FamilyMemberRequest request = FamilyMemberRequest.builder()
            .name("Maria Silva")
            .cpf("12345678909")
            .birthDate(LocalDate.of(1990, 5, 15))
            .relationship("Cônjuge")
            .observations("Sem observações")
            .build();

        mockMvc.perform(post("/api/families/" + familyId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.relationship").value("Cônjuge"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testListMembers() throws Exception {
        FamilyMember member = new FamilyMember();
        member.setFamily(familyRepository.findById(familyId).get());
        member.setName("Maria Silva");
        member.setRelationship("Cônjuge");
        familyMemberRepository.save(member);

        mockMvc.perform(get("/api/families/" + familyId + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testGetMember() throws Exception {
        FamilyMember member = new FamilyMember();
        member.setFamily(familyRepository.findById(familyId).get());
        member.setName("Maria Silva");
        member.setRelationship("Cônjuge");
        member = familyMemberRepository.save(member);

        mockMvc.perform(get("/api/families/" + familyId + "/members/" + member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Silva"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testUpdateMember() throws Exception {
        FamilyMember member = new FamilyMember();
        member.setFamily(familyRepository.findById(familyId).get());
        member.setName("Maria Silva");
        member.setRelationship("Cônjuge");
        member = familyMemberRepository.save(member);

        FamilyMemberRequest request = FamilyMemberRequest.builder()
            .name("Maria Silva Santos")
            .relationship("Cônjuge")
            .build();

        mockMvc.perform(put("/api/families/" + familyId + "/members/" + member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Silva Santos"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testDeleteMember() throws Exception {
        FamilyMember member = new FamilyMember();
        member.setFamily(familyRepository.findById(familyId).get());
        member.setName("Maria Silva");
        member.setRelationship("Cônjuge");
        member = familyMemberRepository.save(member);

        mockMvc.perform(delete("/api/families/" + familyId + "/members/" + member.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testAddMemberToNonExistentFamily() throws Exception {
        FamilyMemberRequest request = FamilyMemberRequest.builder()
            .name("Maria Silva")
            .build();

        mockMvc.perform(post("/api/families/99999/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testGetNonExistentMember() throws Exception {
        mockMvc.perform(get("/api/families/" + familyId + "/members/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testMemberBelongsToWrongFamily() throws Exception {
        Family family2 = new Family();
        family2.setResponsibleName("Pedro Santos");
        family2.setCpf("98765432100");
        family2.setHouseholdSize(3);
        family2.setTotalIncome(new BigDecimal("2000.00"));
        family2.setRequestReason(RequestReason.BAIXA_RENDA);
        family2.setOrigin(FamilyOrigin.CADASTRO_PROPRIO);
        family2.setStatus(FamilyStatus.CADASTRADA);
        family2 = familyRepository.save(family2);

        FamilyMember member = new FamilyMember();
        member.setFamily(family2);
        member.setName("Maria Silva");
        member.setRelationship("Cônjuge");
        member = familyMemberRepository.save(member);

        mockMvc.perform(get("/api/families/" + familyId + "/members/" + member.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "diakono", roles = "DIACONO")
    void testDiakonoCanListMembers() throws Exception {
        mockMvc.perform(get("/api/families/" + familyId + "/members"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "diakono", roles = "DIACONO")
    void testDiakonoCannotAddMember() throws Exception {
        FamilyMemberRequest request = FamilyMemberRequest.builder()
            .name("Maria Silva")
            .build();

        mockMvc.perform(post("/api/families/" + familyId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedCannotAccessMembers() throws Exception {
        mockMvc.perform(get("/api/families/" + familyId + "/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUnauthenticatedCannotAddMember() throws Exception {
        FamilyMemberRequest request = FamilyMemberRequest.builder()
            .name("Maria Silva")
            .build();

        mockMvc.perform(post("/api/families/" + familyId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void testRhCanAddAndUpdateMember() throws Exception {
        FamilyMemberRequest request = FamilyMemberRequest.builder()
            .name("Ana Oliveira")
            .relationship("Filha")
            .build();

        String body = mockMvc.perform(post("/api/families/" + familyId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FamilyMemberResponse created = objectMapper.readValue(body, FamilyMemberResponse.class);

        FamilyMemberRequest update = FamilyMemberRequest.builder()
            .name("Ana Oliveira Santos")
            .relationship("Filha")
            .build();

        mockMvc.perform(put("/api/families/" + familyId + "/members/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Oliveira Santos"));
    }

    @Test
    @WithMockUser(username = "diakono", roles = "DIACONO")
    void testDiakonoCannotUpdateOrDeleteMember() throws Exception {
        FamilyMember member = new FamilyMember();
        member.setFamily(familyRepository.findById(familyId).get());
        member.setName("Maria Silva");
        member.setRelationship("Cônjuge");
        member = familyMemberRepository.save(member);

        FamilyMemberRequest request = FamilyMemberRequest.builder()
            .name("Maria Silva Santos")
            .relationship("Cônjuge")
            .build();

        mockMvc.perform(put("/api/families/" + familyId + "/members/" + member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/families/" + familyId + "/members/" + member.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testAddMemberWithoutName() throws Exception {
        FamilyMemberRequest request = FamilyMemberRequest.builder()
            .name("")
            .build();

        mockMvc.perform(post("/api/families/" + familyId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testListMembersOfNonExistentFamily() throws Exception {
        mockMvc.perform(get("/api/families/99999/members"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testDeleteNonExistentMember() throws Exception {
        mockMvc.perform(delete("/api/families/" + familyId + "/members/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testUpdateMemberBelongsToWrongFamily() throws Exception {
        Family family2 = new Family();
        family2.setResponsibleName("Pedro Santos");
        family2.setCpf("98765432100");
        family2.setHouseholdSize(3);
        family2.setTotalIncome(new BigDecimal("2000.00"));
        family2.setRequestReason(RequestReason.BAIXA_RENDA);
        family2.setOrigin(FamilyOrigin.CADASTRO_PROPRIO);
        family2.setStatus(FamilyStatus.CADASTRADA);
        family2 = familyRepository.save(family2);

        FamilyMember member = new FamilyMember();
        member.setFamily(family2);
        member.setName("Maria Silva");
        member.setRelationship("Cônjuge");
        member = familyMemberRepository.save(member);

        FamilyMemberRequest request = FamilyMemberRequest.builder()
            .name("Maria Silva Santos")
            .relationship("Cônjuge")
            .build();

        mockMvc.perform(put("/api/families/" + familyId + "/members/" + member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
