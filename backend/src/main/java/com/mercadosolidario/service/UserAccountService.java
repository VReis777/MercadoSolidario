package com.mercadosolidario.service;

import com.mercadosolidario.dto.auth.CreateUserInvitationRequest;
import com.mercadosolidario.dto.auth.SetPasswordRequest;
import com.mercadosolidario.dto.auth.UserInvitationResponse;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.exception.InvitationEmailUnavailableException;
import com.mercadosolidario.model.PasswordSetupToken;
import com.mercadosolidario.model.Role;
import com.mercadosolidario.model.User;
import com.mercadosolidario.repository.PasswordSetupTokenRepository;
import com.mercadosolidario.repository.RoleRepository;
import com.mercadosolidario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.net.URI;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private static final Set<String> ASSIGNABLE_ROLES = Set.of("RH_TESOURARIA", "DIACONO");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /** Evita reenviar e-mails a cada tentativa de login com a senha temporária. */
    private static final int RESEND_COOLDOWN_MINUTES = 2;

    public enum PasswordLinkOutcome { SENT, ALREADY_SENT, EMAIL_UNAVAILABLE }

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordSetupTokenRepository passwordSetupTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<InvitationEmailSender> invitationEmailSenderProvider;

    @Value("${app.frontend-base-url:}")
    private String frontendBaseUrl;

    @Value("${app.user-management.owner-username:}")
    private String ownerUsername;

    @Value("${app.users.temporary-password:}")
    private String temporaryPassword;

    @Value("${app.users.password-link-minutes:30}")
    private int passwordLinkMinutes;

    /** Cria o perfil com a senha temporária padrão; a troca é exigida no primeiro login. */
    @Transactional
    public UserInvitationResponse invite(CreateUserInvitationRequest request, String actorUsername) {
        if (ownerUsername.isBlank()
                || !ownerUsername.equalsIgnoreCase(actorUsername)) {
            throw new AccessDeniedException("Somente o administrador responsável pode cadastrar pessoas.");
        }

        String roleName = request.role().trim();
        if (!ASSIGNABLE_ROLES.contains(roleName)) {
            throw new IllegalArgumentException("Este perfil não pode ser atribuído por este cadastro.");
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("O perfil solicitado não está disponível."));

        User user = createWithTemporaryPassword(request.fullName(), request.email(), role);
        return new UserInvitationResponse(user.getId(), user.getFullName(), user.getEmail(), roleName);
    }

    @Transactional
    public User createWithTemporaryPassword(String fullName, String rawEmail, Role role) {
        if (temporaryPassword == null || temporaryPassword.length() < 8) {
            throw new IllegalStateException("Senha temporária não configurada (mínimo de 8 caracteres).");
        }
        String email = normalizeEmail(rawEmail);
        if (userRepository.existsByUsername(email) || userRepository.existsByEmail(email)) {
            throw new ConflictException("Já existe uma conta com este e-mail.");
        }
        User user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setFullName(fullName.trim());
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    /**
     * Gera um link individual de troca (uso único, expira em minutos) e envia ao e-mail do perfil.
     * Nunca afirma envio quando o SMTP não está configurado.
     */
    @Transactional(noRollbackFor = InvitationEmailUnavailableException.class)
    public PasswordLinkOutcome sendFirstAccessLink(User user) {
        LocalDateTime now = LocalDateTime.now();
        Optional<PasswordSetupToken> existing = passwordSetupTokenRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            PasswordSetupToken old = existing.get();
            if (old.getUsedAt() == null && old.getExpiresAt().isAfter(now)
                    && old.getCreatedAt() != null && old.getCreatedAt().isAfter(now.minusMinutes(RESEND_COOLDOWN_MINUTES))) {
                return PasswordLinkOutcome.ALREADY_SENT;
            }
            passwordSetupTokenRepository.delete(old);
            passwordSetupTokenRepository.flush();
        }

        InvitationEmailSender emailSender = invitationEmailSenderProvider.getIfAvailable();
        if (emailSender == null) {
            return PasswordLinkOutcome.EMAIL_UNAVAILABLE;
        }
        final String baseUrl;
        try {
            baseUrl = validatedFrontendBaseUrl();
        } catch (InvitationEmailUnavailableException exception) {
            return PasswordLinkOutcome.EMAIL_UNAVAILABLE;
        }

        String rawToken = generateSecret();
        PasswordSetupToken token = new PasswordSetupToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(now.plusMinutes(passwordLinkMinutes));
        passwordSetupTokenRepository.save(token);

        try {
            emailSender.sendPasswordChangeLink(user.getEmail(), user.getFullName(),
                    baseUrl + "/definir-senha?token=" + rawToken, passwordLinkMinutes);
            return PasswordLinkOutcome.SENT;
        } catch (InvitationEmailUnavailableException exception) {
            passwordSetupTokenRepository.delete(token);
            return PasswordLinkOutcome.EMAIL_UNAVAILABLE;
        }
    }

    @Transactional
    public void setPassword(SetPasswordRequest request) {
        PasswordSetupToken setupToken = passwordSetupTokenRepository
                .findByTokenHashForUpdate(hashToken(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("O link é inválido, expirou ou já foi utilizado."));

        LocalDateTime now = LocalDateTime.now();
        if (setupToken.getUsedAt() != null || !setupToken.getExpiresAt().isAfter(now)
                || !setupToken.getUser().getActive()) {
            throw new IllegalArgumentException("O link é inválido, expirou ou já foi utilizado.");
        }

        User user = setupToken.getUser();
        String newPassword = request.newPassword();
        if (newPassword.equals(temporaryPassword) || passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha temporária.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(now);
        setupToken.setUsedAt(now);
        userRepository.save(user);
        passwordSetupTokenRepository.save(setupToken);
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private String validatedFrontendBaseUrl() {
        final URI uri;
        try {
            uri = URI.create(frontendBaseUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvitationEmailUnavailableException("A URL do frontend para convites é inválida.");
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        boolean localHost = host != null && Set.of("127.0.0.1", "localhost", "::1")
                .contains(host.toLowerCase(Locale.ROOT));
        boolean privateLan = host != null && (
                host.startsWith("192.168.")
                || host.startsWith("10.")
                || host.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"));
        if (!uri.isAbsolute()
                || host == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !("https".equalsIgnoreCase(scheme)
                || ("http".equalsIgnoreCase(scheme) && (localHost || privateLan)))) {
            throw new InvitationEmailUnavailableException(
                    "A URL de convites deve usar HTTPS, exceto em loopback local ou rede privada.");
        }
        return uri.toString().replaceAll("/+$", "");
    }
}
