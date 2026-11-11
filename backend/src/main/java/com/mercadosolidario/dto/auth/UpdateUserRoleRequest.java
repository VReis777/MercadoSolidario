package com.mercadosolidario.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRoleRequest(
        @NotBlank @Pattern(regexp = "ADMIN|RH_TESOURARIA|DIACONO") String role
) {
}
