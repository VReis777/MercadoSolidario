package com.mercadosolidario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.model.PasswordSetupToken;
import com.mercadosolidario.model.Role;
import com.mercadosolidario.model.User;
import com.mercadosolidario.repository.PasswordSetupTokenRepository;
import com.mercadosolidario.repository.RoleRepository;
import com.mercadosolidario.repository.UserRepository;
import com.mercadosolidario.service.InvitationEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.time.LocalDateTime;

import com.mercadosolidario.exception.InvitationEmailUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.frontend-base-url=http://127.0.0.1:5173",
        "app.user-management.owner-username=owner@example.org"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserAccountManagementTest {

    private static final String TEMPORARY_PASSWORD = "senh@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @MockBean
    private InvitationEmailSender invitationEmailSender;

    @Autowired
    private com.mercadosolidario.service.UserAccountService userAccountService;

    @BeforeEach
    void setUp() {
        for (String roleName : new String[]{"ADMIN", "RH_TESOURARIA", "DIACONO"}) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = new Role();
                role.setName(roleName);
                role.setDescription(roleName);
                roleRepository.save(role);
            }
        }
    }

    @Test
    void adminCreatesProfileWithTemporaryPasswordAndFirstLoginRequiresEmailLink() throws Exception {
        String response = mockMvc.perform(post("/api/admin/users")
                        .with(user("owner@example.org").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Pessoa Convidada",
                                "email", "New.Person@Example.org",
                                "role", "RH_TESOURARIA"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.person@example.org"))
                .andExpect(jsonPath("$.role").value("RH_TESOURARIA"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verifyNoInteractions(invitationEmailSender);
        User invitedUser = userRepository.findByEmail("new.person@example.org").orElseThrow();
        assertThat(invitedUser.getMustChangePassword()).isTrue();
        assertThat(passwordEncoder.matches(TEMPORARY_PASSWORD, invitedUser.getPassword())).isTrue();
        assertThat(objectMapper.readTree(response).get("id").asLong()).isEqualTo(invitedUser.getId());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("new.person@example.org", TEMPORARY_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(true))
                .andExpect(jsonPath("$.emailSent").value(true))
                .andExpect(jsonPath("$.token").doesNotExist());

        String token = capturedChangeToken("new.person@example.org");
        assertThat(passwordSetupTokenRepository.findAll())
                .singleElement()
                .satisfies(t -> {
                    assertThat(t.getTokenHash()).isNotEqualTo(token);
                    assertThat(t.getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(31));
                });

        mockMvc.perform(post("/api/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setPasswordBody(token, TEMPORARY_PASSWORD)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setPasswordBody(token, "1234567")))
                .andExpect(status().isBadRequest());

        String passwordRequest = setPasswordBody(token, "minhasenha");
        mockMvc.perform(post("/api/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest))
                .andExpect(status().isBadRequest());

        invitedUser = userRepository.findByEmail("new.person@example.org").orElseThrow();
        assertThat(invitedUser.getMustChangePassword()).isFalse();
        assertThat(invitedUser.getPasswordChangedAt()).isNotNull();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("NEW.person@example.org", "minhasenha")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.passwordChangeRequired").value(false))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_RH_TESOURARIA"));
    }

    @Test
    void repeatedFirstLoginDoesNotResendWithinCooldown() throws Exception {
        createProfile("cooldown.person@example.org");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("cooldown.person@example.org", TEMPORARY_PASSWORD)))
                .andExpect(jsonPath("$.emailSent").value(true));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("cooldown.person@example.org", TEMPORARY_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("já foi enviado")));
        verify(invitationEmailSender, times(1)).sendPasswordChangeLink(any(), any(), any(), anyInt());
    }

    @Test
    void firstLoginWithoutSmtpDoesNotClaimEmailWasSent() throws Exception {
        createProfile("nosmtp.person@example.org");
        doThrow(new InvitationEmailUnavailableException("Envio de e-mail não configurado."))
                .when(invitationEmailSender).sendPasswordChangeLink(any(), any(), any(), anyInt());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nosmtp.person@example.org", TEMPORARY_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(true))
                .andExpect(jsonPath("$.emailSent").value(false))
                .andExpect(jsonPath("$.token").doesNotExist());
        assertThat(passwordSetupTokenRepository.findAll()).isEmpty();
    }

    @Test
    void wrongPasswordIsRejectedWithoutSendingLink() throws Exception {
        createProfile("wrong.person@example.org");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("wrong.person@example.org", "outrasenha")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(invitationEmailSender);
    }

    @Test
    void initialAdminIsCreatedOnceAndNeverReset() throws Exception {
        var initialAdminSetup = new com.mercadosolidario.config.InitialAdminSetup(
                userRepository, roleRepository, userAccountService);
        org.springframework.test.util.ReflectionTestUtils.setField(initialAdminSetup, "email", " Admin.Inicial@Example.org ");
        org.springframework.test.util.ReflectionTestUtils.setField(initialAdminSetup, "name", "Admin Inicial");
        initialAdminSetup.run(null);

        User admin = userRepository.findByEmail("admin.inicial@example.org").orElseThrow();
        assertThat(admin.getMustChangePassword()).isTrue();
        assertThat(admin.getRoles()).extracting(Role::getName).containsExactly("ADMIN");
        assertThat(passwordEncoder.matches(TEMPORARY_PASSWORD, admin.getPassword())).isTrue();

        admin.setPassword(passwordEncoder.encode("senha-ja-trocada"));
        admin.setMustChangePassword(false);
        userRepository.saveAndFlush(admin);
        initialAdminSetup.run(null);

        User again = userRepository.findByEmail("admin.inicial@example.org").orElseThrow();
        assertThat(passwordEncoder.matches("senha-ja-trocada", again.getPassword())).isTrue();
        assertThat(again.getMustChangePassword()).isFalse();
    }

    private void createProfile(String email) throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .with(user("owner@example.org").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Pessoa Teste", "email", email, "role", "DIACONO"))))
                .andExpect(status().isCreated());
    }

    private String capturedChangeToken(String email) {
        var emailLink = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(invitationEmailSender).sendPasswordChangeLink(eq(email), any(), emailLink.capture(), eq(30));
        assertThat(emailLink.getValue()).startsWith("http://127.0.0.1:5173/definir-senha?token=");
        return UriComponentsBuilder.fromUriString(emailLink.getValue()).build().getQueryParams().getFirst("token");
    }

    private String loginBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    private String setPasswordBody(String token, String newPassword) throws Exception {
        return objectMapper.writeValueAsString(Map.of("token", token, "newPassword", newPassword));
    }

    @Test
    void userManagementRequiresAdminRole() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", "Pessoa Convidada",
                "email", "new.person@example.org",
                "role", "DIACONO"));

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/users")
                        .with(user("rh@example.org").roles("RH_TESOURARIA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsByEmail("new.person@example.org")).isFalse();
    }

    @Test
    void anotherAdminCannotManageUserInvitations() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .with(user("another-admin@example.org").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Pessoa Convidada",
                                "email", "new.person@example.org",
                                "role", "DIACONO"))))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsByEmail("new.person@example.org")).isFalse();
    }

    @Test
    void invitationCannotCreateAnotherAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .with(user("owner@example.org").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "New administrator",
                                "email", "admin@example.org",
                                "role", "ADMIN"))))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.existsByEmail("admin@example.org")).isFalse();
    }

    @Test
    void expiredInvitationLinkCannotSetPassword() throws Exception {
        createProfile("expired.person@example.org");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("expired.person@example.org", TEMPORARY_PASSWORD)))
                .andExpect(jsonPath("$.passwordChangeRequired").value(true));
        String token = capturedChangeToken("expired.person@example.org");

        PasswordSetupToken setupToken = passwordSetupTokenRepository.findAll().getFirst();
        assertThat(setupToken.getExpiresAt()).isAfter(LocalDateTime.now());
        setupToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        passwordSetupTokenRepository.saveAndFlush(setupToken);

        mockMvc.perform(post("/api/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", token,
                                "newPassword", "ChangedPassword123!"))))
                .andExpect(status().isBadRequest());
    }
}
