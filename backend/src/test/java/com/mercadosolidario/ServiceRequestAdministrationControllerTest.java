package com.mercadosolidario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.model.*;
import com.mercadosolidario.repository.*;
import com.mercadosolidario.service.ServiceRequestProtocol;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ServiceRequestAdministrationControllerTest {
    private static final String ADMIN_API = "/api/service-requests";
    private static final String LOOKUP_API = "/api/public/service-requests/lookup";
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PublicServiceRequestRepository requests;
    @Autowired private PublicServiceRequestHistoryRepository history;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;

    @BeforeEach
    void resetRequests() {
        history.deleteAll();
        requests.deleteAll();
    }

    @Test
    void lookupRequiresBothFactorsAndDoesNotEnumerateOrExposeRequestDetails() throws Exception {
        String createdResponse = mockMvc.perform(post("/api/public/service-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Pessoa Protegida","contactValue":"Maria@Example.com","contactPreference":"EMAIL",
                     "location":"Rua Reservada","householdSize":3,"assistanceType":"FOOD",
                     "message":"Informação privada","consentAccepted":true}
                    """))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String rawProtocol = objectMapper.readTree(createdResponse).get("protocol").asText();

        mockMvc.perform(post(LOOKUP_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "protocol", rawProtocol, "contactValue", "  MARIA@example.com "))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RECEBIDA"))
            .andExpect(jsonPath("$.contactMasked").value("M***@Example.com"))
            .andExpect(jsonPath("$.createdAt").isString())
            .andExpect(jsonPath("$.updatedAt").isString())
            .andExpect(jsonPath("$.assistanceType").doesNotExist())
            .andExpect(jsonPath("$.message").doesNotExist())
            .andExpect(jsonPath("$.location").doesNotExist())
            .andExpect(jsonPath("$.fullName").doesNotExist())
            .andExpect(jsonPath("$.contactValue").doesNotExist());

        var wrongFactor = mockMvc.perform(post(LOOKUP_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(lookupJson(rawProtocol, "wrong@example.com")))
            .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        var missingRequest = mockMvc.perform(post(LOOKUP_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(lookupJson("b".repeat(64), "wrong@example.com")))
            .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(wrongFactor).isEqualTo(missingRequest);
    }

    @Test
    void adminApisRequireAuthenticationAndAuthorizedRole() throws Exception {
        mockMvc.perform(get(ADMIN_API)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADMIN_API).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user("outsider").roles("OUTSIDER")))
            .andExpect(status().isForbidden());
        mockMvc.perform(post(ADMIN_API + "/1/notes")
                .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"internal\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanFilterTransitionAnnotateAssignAndReadAuditedHistory() throws Exception {
        PublicServiceRequest request = saveRequest("c".repeat(64), ServiceRequestStatus.RECEBIDA);
        User assignee = saveUser("rh-operator", "RH_TESOURARIA");

        mockMvc.perform(get(ADMIN_API)
                .param("status", "RECEBIDA")
                .param("protocol", "c".repeat(64))
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(request.getId()))
            .andExpect(jsonPath("$.content[0].protocol").doesNotExist())
            .andExpect(jsonPath("$.content[0].contact").value("+55 (11) 99999-9999"))
            .andExpect(jsonPath("$.content[0].updatedAt").isString())
            .andExpect(jsonPath("$.content[0].message").doesNotExist())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.pageable.pageNumber").value(0));

        mockMvc.perform(patch(ADMIN_API + "/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newStatus\":\"EM_ANALISE\",\"observation\":\"Triagem concluída\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_ANALISE"));
        mockMvc.perform(patch(ADMIN_API + "/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newStatus\":\"ENCAMINHADA\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENCAMINHADA"));
        mockMvc.perform(post(ADMIN_API + "/" + request.getId() + "/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"observation\":\"Nota interna restrita\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post(ADMIN_API + "/" + request.getId() + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + assignee.getUsername() + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedUsername").value("rh-operator"));

        mockMvc.perform(get(ADMIN_API + "/" + request.getId() + "/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].changedBy").value("admin"))
            .andExpect(jsonPath("$[0].changedAt").isString())
            .andExpect(jsonPath("$[2].id").isNumber())
            .andExpect(jsonPath("$[2].observation").value("Nota interna restrita"));
        assertThat(history.findByServiceRequestIdOrderByCreatedAtAscIdAsc(request.getId()))
            .hasSize(4);
    }

    @Test
    @WithMockUser(username = "rh-user", roles = "RH_TESOURARIA")
    void rhCanViewAndForwardButCannotWriteNotesOrMakeOtherTransitions() throws Exception {
        PublicServiceRequest request = saveRequest("d".repeat(64), ServiceRequestStatus.EM_ANALISE);
        mockMvc.perform(get(ADMIN_API + "/" + request.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").doesNotExist())
            .andExpect(jsonPath("$.contactValue").doesNotExist())
            .andExpect(jsonPath("$.contact").value("••••••99"));
        mockMvc.perform(patch(ADMIN_API + "/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"ENCAMINHADA\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(patch(ADMIN_API + "/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"CANCELADA\"}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post(ADMIN_API + "/" + request.getId() + "/notes")
                .contentType(MediaType.APPLICATION_JSON).content("{\"observation\":\"forbidden\"}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(ADMIN_API + "/" + request.getId() + "/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].observation").doesNotExist());
    }

    @Test
    @WithMockUser(username = "diacono-user", roles = "DIACONO")
    void diaconoGetsMinimizedDetailsAndOnlyInitialTriageTransition() throws Exception {
        PublicServiceRequest request = saveRequest("e".repeat(64), ServiceRequestStatus.RECEBIDA);
        request.setAssignedUser(saveUser("diacono-user", "DIACONO"));
        requests.saveAndFlush(request);
        PublicServiceRequest unassignedRequest = saveRequest("8".repeat(64), ServiceRequestStatus.RECEBIDA);

        mockMvc.perform(get(ADMIN_API).param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(request.getId()));
        // O diácono atribuído vê contato e dados da família para conseguir acolher; mensagem segue restrita a ADMIN.
        mockMvc.perform(get(ADMIN_API + "/" + request.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Pessoa protegida"))
            .andExpect(jsonPath("$.location").value("Local protegido"))
            .andExpect(jsonPath("$.householdSize").value(4))
            .andExpect(jsonPath("$.message").doesNotExist())
            .andExpect(jsonPath("$.contact").value("+55 (11) 99999-9999"));
        mockMvc.perform(get(ADMIN_API + "/" + unassignedRequest.getId()))
            .andExpect(status().isNotFound());
        mockMvc.perform(patch(ADMIN_API + "/" + unassignedRequest.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"EM_ANALISE\"}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(patch(ADMIN_API + "/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"EM_ANALISE\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(patch(ADMIN_API + "/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"ENCAMINHADA\"}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post(ADMIN_API + "/" + request.getId() + "/assign")
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"rh-operator\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rejectsInvalidTransitionsAndUnsuitableAssignmentTargets() throws Exception {
        PublicServiceRequest request = saveRequest("f".repeat(64), ServiceRequestStatus.RECEBIDA);
        mockMvc.perform(patch(ADMIN_API + "/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"ATENDIDA\"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post(ADMIN_API + "/" + request.getId() + "/assign")
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"missing\"}"))
            .andExpect(status().isBadRequest());
        saveUser("unrelated", "UNRELATED");
        mockMvc.perform(post(ADMIN_API + "/" + request.getId() + "/assign")
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"unrelated\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanUseSupportedWorkflowStatusesAndTerminalStatusesCannotAdvance() throws Exception {
        PublicServiceRequest request = saveRequest("9".repeat(64), ServiceRequestStatus.EM_ANALISE);

        mockMvc.perform(patch(ADMIN_API + "/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newStatus\":\"AGUARDANDO_CONTATO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AGUARDANDO_CONTATO"));

        mockMvc.perform(patch(ADMIN_API + "/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newStatus\":\"ATENDIDA\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ATENDIDA"));

        mockMvc.perform(patch(ADMIN_API + "/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newStatus\":\"CANCELADA\"}"))
            .andExpect(status().isBadRequest());
    }

    private PublicServiceRequest saveRequest(String protocol, ServiceRequestStatus status) {
        PublicServiceRequest request = new PublicServiceRequest();
        request.setProtocolHash(ServiceRequestProtocol.hash(protocol));
        request.setFullName("Pessoa protegida");
        request.setContactValue("+55 (11) 99999-9999");
        request.setContactPreference(ServiceRequestContactPreference.PHONE);
        request.setLocation("Local protegido");
        request.setHouseholdSize(4);
        request.setAssistanceType("FOOD");
        request.setMessage("Mensagem interna");
        request.setConsentAccepted(true);
        request.setStatus(status);
        return requests.saveAndFlush(request);
    }

    private User saveUser(String username, String role) {
        Role assignedRole = roles.findByName(role).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(role);
            newRole.setDescription(role);
            return roles.saveAndFlush(newRole);
        });
        User user = new User();
        user.setUsername(username);
        user.setPassword("unused");
        user.setEmail(username + "@example.com");
        user.setFullName(username);
        user.setActive(true);
        user.setRoles(Set.of(assignedRole));
        return users.saveAndFlush(user);
    }

    private String lookupJson(String protocol, String contact) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("protocol", protocol, "contactValue", contact));
    }

}
