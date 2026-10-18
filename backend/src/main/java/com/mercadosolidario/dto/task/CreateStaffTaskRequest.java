package com.mercadosolidario.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStaffTaskRequest {

    @NotNull
    private Long assigneeId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;
}
