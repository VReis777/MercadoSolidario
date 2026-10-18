package com.mercadosolidario.security;

import com.mercadosolidario.dto.auth.LoginRequest;
import com.mercadosolidario.dto.auth.LoginResponse;
import com.mercadosolidario.model.User;
import com.mercadosolidario.repository.UserRepository;
import com.mercadosolidario.service.UserAccountService;
import com.mercadosolidario.service.UserAccountService.PasswordLinkOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserAccountService userAccountService;

    @Value("${jwt.expiration}")
    private Long jwtExpirationMillis;

    @Value("${app.users.password-link-minutes:30}")
    private int passwordLinkMinutes;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        if (username.contains("@")) {
            username = UserAccountService.normalizeEmail(username);
        }
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.getPassword()));

        User user = userRepository.findByUsername(username).orElseThrow();
        if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            return firstAccessResponse(user, userAccountService.sendFirstAccessLink(user));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return LoginResponse.builder()
                .token(jwtService.generateToken(userDetails))
                .type("Bearer")
                .expiresIn(jwtExpirationMillis / 1000)
                .user(LoginResponse.UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .roles(user.getRoles().stream()
                                .map(role -> "ROLE_" + role.getName())
                                .collect(Collectors.toList()))
                        .build())
                .build();
    }

    private LoginResponse firstAccessResponse(User user, PasswordLinkOutcome outcome) {
        String masked = maskEmail(user.getEmail());
        String message = switch (outcome) {
            case SENT -> "Primeiro acesso: enviamos para " + masked + " um link para criar sua nova senha (válido por "
                    + passwordLinkMinutes + " minutos). Depois de trocar, entre com a nova senha.";
            case ALREADY_SENT -> "Um link de troca de senha já foi enviado há instantes para " + masked
                    + ". Verifique a caixa de entrada e o spam.";
            case EMAIL_UNAVAILABLE -> "Primeiro acesso: é preciso trocar a senha temporária, mas o envio de e-mail "
                    + "ainda não está configurado no sistema. Nenhum e-mail foi enviado; procure o administrador.";
        };
        return LoginResponse.builder()
                .passwordChangeRequired(true)
                .emailSent(outcome != PasswordLinkOutcome.EMAIL_UNAVAILABLE)
                .maskedEmail(masked)
                .message(message)
                .build();
    }

    static String maskEmail(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        if (at < 1) return "o e-mail cadastrado";
        return email.charAt(0) + "*".repeat(Math.max(at - 1, 3)) + email.substring(at);
    }
}
