package com.mercadosolidario.dto.task;

import java.time.LocalDateTime;

public record StaffTaskResponse(
        Long id,
        String title,
        String description,
        Long assigneeId,
        String assigneeName,
        String assigneeEmail,
        String createdByName,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        boolean emailSent) {
}
