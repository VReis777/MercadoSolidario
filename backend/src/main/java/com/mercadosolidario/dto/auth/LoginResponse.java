package com.mercadosolidario.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type;
    private Long expiresIn;
    private UserResponse user;

    /** Primeiro acesso: nenhum token é emitido até a senha temporária ser trocada pelo link enviado por e-mail. */
    private boolean passwordChangeRequired;
    private Boolean emailSent;
    private String maskedEmail;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String username;
        private String fullName;
        private List<String> roles;
    }
}
