package com.mercadosolidario.controller;

import com.mercadosolidario.dto.auth.ChangePasswordRequest;
import com.mercadosolidario.dto.auth.LoginRequest;
import com.mercadosolidario.dto.auth.LoginResponse;
import com.mercadosolidario.dto.auth.SetPasswordRequest;
import com.mercadosolidario.model.User;
import com.mercadosolidario.repository.UserRepository;
import com.mercadosolidario.security.AuthenticationService;
import com.mercadosolidario.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountService userAccountService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/set-password")
    public ResponseEntity<Void> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        userAccountService.setPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user (JWT stateless - client should remove token)")
    public ResponseEntity<Void> logout() {
        // JWT is stateless, so we just return success
        // Client should remove the token from storage
        return ResponseEntity.ok().build();
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change current user's password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 8 caracteres.");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    @Operation(summary = "Get current user information")
    public ResponseEntity<LoginResponse.UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(
                LoginResponse.UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .roles(user.getRoles().stream()
                                .map(role -> "ROLE_" + role.getName())
                                .collect(java.util.stream.Collectors.toList()))
                        .build()
        );
    }
}
