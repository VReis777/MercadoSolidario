package com.mercadosolidario.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserInvitationRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Pattern(regexp = "RH_TESOURARIA|DIACONO") String role
) {
}
