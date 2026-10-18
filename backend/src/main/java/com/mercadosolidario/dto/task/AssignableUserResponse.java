package com.mercadosolidario.dto.task;

import java.util.List;

public record AssignableUserResponse(
        Long id,
        String fullName,
        String email,
        List<String> roles) {
}
