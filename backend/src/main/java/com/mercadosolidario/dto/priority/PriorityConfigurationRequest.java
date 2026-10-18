package com.mercadosolidario.dto.priority;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PriorityConfigurationRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,80}$")
    private String code;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 2000)
    private String explanation;

    @NotEmpty
    @Valid
    private List<PriorityFactorRequest> factors;
}
