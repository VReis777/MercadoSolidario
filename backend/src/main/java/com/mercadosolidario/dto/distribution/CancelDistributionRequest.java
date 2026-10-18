package com.mercadosolidario.dto.distribution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelDistributionRequest {

    @NotBlank
    @Size(max = 2000)
    private String reason;
}
