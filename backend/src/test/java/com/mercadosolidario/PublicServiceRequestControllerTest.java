package com.mercadosolidario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.dto.publicrequest.CreatePublicServiceRequest;
import com.mercadosolidario.model.PublicServiceRequest;
import com.mercadosolidario.model.ServiceRequestContactPreference;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.PublicServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicServiceRequestControllerTest {

    private static final String ENDPOINT = "/api/public/service-requests";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PublicServiceRequestRepository serviceRequestRepository;
    @Autowired private FamilyRepository familyRepository;

    @Test
    void anonymousCanCreateRequestUsingPublicContractAndReceivesOneTimeRandomProtocol() throws Exception {
        CreatePublicServiceRequest request = validRequest();

        String response = mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.protocol").isString())
            .andExpect(jsonPath("$.fullName").doesNotExist())
            .andExpect(jsonPath("$.contactValue").doesNotExist())
            .andExpect(jsonPath("$.contactPreference").doesNotExist())
            .andExpect(jsonPath("$.location").doesNotExist())
            .andExpect(jsonPath("$.message").doesNotExist())
            .andReturn().getResponse().getContentAsString();

        String protocol = objectMapper.readTree(response).get("protocol").asText();
        assertThat(protocol).matches("[0-9a-f]{64}");
        assertThat(serviceRequestRepository.count()).isEqualTo(1);
        PublicServiceRequest saved = serviceRequestRepository.findAll().get(0);
        assertThat(saved.getProtocolHash()).hasSize(64).isNotEqualTo(protocol);
        assertThat(saved.getLocation()).isEqualTo("São Paulo, Centro");
        assertThat(saved.getHouseholdSize()).isEqualTo(2);
        assertThat(saved.getConsentAccepted()).isTrue();
        assertThat(familyRepository.count()).isZero();

        String secondResponse = mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(secondResponse).get("protocol").asText()).isNotEqualTo(protocol);
    }

    @Test
    void validatesContactValueAccordingToSelectedPreference() throws Exception {
        CreatePublicServiceRequest emailRequest = validRequest();
        emailRequest.setContactValue("maria@example.com");
        emailRequest.setContactPreference(ServiceRequestContactPreference.EMAIL);

        mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailRequest)))
            .andExpect(status().isCreated());
        assertThat(serviceRequestRepository.findAll().get(0).getContactValue()).isEqualTo("maria@example.com");

        CreatePublicServiceRequest phoneRequest = validRequest();
        phoneRequest.setContactValue("+55 (11) 99999-9999");
        phoneRequest.setContactPreference(ServiceRequestContactPreference.PHONE);

        mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(phoneRequest)))
            .andExpect(status().isCreated());
        assertThat(serviceRequestRepository.count()).isEqualTo(2);
    }

    @Test
    void invalidRequestsReturnSanitizedJsonWithoutPersistingAnything() throws Exception {
        CreatePublicServiceRequest invalidQuantity = validRequest();
        invalidQuantity.setHouseholdSize(0);
        CreatePublicServiceRequest missingConsent = validRequest();
        missingConsent.setConsentAccepted(false);
        CreatePublicServiceRequest invalidEmail = validRequest();
        invalidEmail.setContactPreference(ServiceRequestContactPreference.EMAIL);
        invalidEmail.setContactValue("invalid-email");
        CreatePublicServiceRequest invalidPhone = validRequest();
        invalidPhone.setContactPreference(ServiceRequestContactPreference.PHONE);
        invalidPhone.setContactValue("not-a-phone");
        CreatePublicServiceRequest messageTooLong = validRequest();
        messageTooLong.setMessage("x".repeat(1001));

        for (CreatePublicServiceRequest request : new CreatePublicServiceRequest[] {
                invalidQuantity, missingConsent, invalidEmail, invalidPhone, messageTooLong
        }) {
            mockMvc.perform(post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
        }

        mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(serviceRequestRepository.count()).isZero();
        assertThat(familyRepository.count()).isZero();
    }

    @Test
    void welcomeVisitRequiresAnAvailableSlotAndIsStored() throws Exception {
        java.time.LocalDate day = java.time.LocalDate.now().plusDays(7);
        com.mercadosolidario.model.WelcomeAvailability slot =
            new com.mercadosolidario.model.WelcomeAvailability();
        slot.setAvailableDate(day);
        slot.setActive(true);
        welcomeRepository.save(slot);

        CreatePublicServiceRequest request = validRequest();
        request.setWelcomePreference(com.mercadosolidario.model.ServiceRequestWelcomePreference.VISIT);
        request.setWelcomeDate(day);
        request.setWelcomeTime("14:30");

        mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        PublicServiceRequest saved = serviceRequestRepository.findAll().get(0);
        assertThat(saved.getWelcomePreference())
            .isEqualTo(com.mercadosolidario.model.ServiceRequestWelcomePreference.VISIT);
        assertThat(saved.getWelcomeDate()).isEqualTo(day);
        assertThat(saved.getWelcomeTime()).isEqualTo("14:30");
    }

    @Test
    void welcomeVisitOnUnavailableDateIsRejected() throws Exception {
        CreatePublicServiceRequest noSlot = validRequest();
        noSlot.setWelcomePreference(com.mercadosolidario.model.ServiceRequestWelcomePreference.VISIT);
        noSlot.setWelcomeDate(java.time.LocalDate.now().plusDays(10));

        CreatePublicServiceRequest missingDate = validRequest();
        missingDate.setWelcomePreference(com.mercadosolidario.model.ServiceRequestWelcomePreference.VISIT);

        CreatePublicServiceRequest badTime = validRequest();
        badTime.setWelcomePreference(com.mercadosolidario.model.ServiceRequestWelcomePreference.VISIT);
        badTime.setWelcomeDate(java.time.LocalDate.now().plusDays(5));
        badTime.setWelcomeTime("25:00");

        for (CreatePublicServiceRequest request : new CreatePublicServiceRequest[] { noSlot, missingDate, badTime }) {
            mockMvc.perform(post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
        assertThat(serviceRequestRepository.count()).isZero();
    }

    @Test
    void phoneWelcomeIsAcceptedWithoutDateAndPublicSlotsEndpointIsOpen() throws Exception {
        CreatePublicServiceRequest request = validRequest();
        request.setWelcomePreference(com.mercadosolidario.model.ServiceRequestWelcomePreference.PHONE);

        mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
        assertThat(serviceRequestRepository.findAll().get(0).getWelcomePreference())
            .isEqualTo(com.mercadosolidario.model.ServiceRequestWelcomePreference.PHONE);

        mockMvc.perform(get("/api/public/welcome-slots"))
            .andExpect(status().isOk());
    }

    @Autowired private com.mercadosolidario.repository.WelcomeAvailabilityRepository welcomeRepository;

    @Test
    void companyContactIsRegisteredAsSponsorAndSurvivesWithoutSmtp() throws Exception {
        String body = """
            {"responsibleName":"Maria Silva","companyName":"Padaria Estrela",
             "email":"maria@padaria.com","phone":"24999998888","whatsapp":"24999998888",
             "interest":"SER_PARCEIRO","message":"Queremos apoiar."}
            """;

        mockMvc.perform(post("/api/public/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.registered").value(true))
            .andExpect(jsonPath("$.emailSent").isBoolean());

        var sponsors = sponsorRepository.findAll();
        assertThat(sponsors).hasSize(1);
        var sponsor = sponsors.get(0);
        assertThat(sponsor.getCompanyName()).isEqualTo("Padaria Estrela");
        assertThat(sponsor.getName()).isEqualTo("Maria Silva");
        assertThat(sponsor.getType().name()).isEqualTo("EMPRESA");
        assertThat(sponsor.getObservations()).contains("parceiro");
    }

    @Test
    void companyContactRejectsInvalidInterestAndSiteInfoIsPublic() throws Exception {
        mockMvc.perform(post("/api/public/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"responsibleName":"João","companyName":"Loja X",
                     "email":"joao@loja.com","interest":"ESPIAO"}
                    """))
            .andExpect(status().isBadRequest());
        assertThat(sponsorRepository.count()).isZero();

        mockMvc.perform(get("/api/public/site-info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instagramUrl").isString());
    }

    @Autowired private com.mercadosolidario.repository.SponsorRepository sponsorRepository;

    @Test
    void doesNotExposePublicReadEndpoint() throws Exception {
        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isMethodNotAllowed());
        assertThat(serviceRequestRepository.count()).isZero();
    }

    private CreatePublicServiceRequest validRequest() {
        CreatePublicServiceRequest request = new CreatePublicServiceRequest();
        request.setLocation("São Paulo, Centro");
        request.setHouseholdSize(2);
        request.setAssistanceType("Alimentação");
        request.setMessage("Solicito auxílio.");
        request.setConsentAccepted(true);
        return request;
    }
}
