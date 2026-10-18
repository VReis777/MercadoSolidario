package com.mercadosolidario.dto.distribution;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateDistributionRequest {

    @NotNull
    private Long familyId;

    @NotEmpty
    @Size(max = 50)
    @Valid
    private List<DistributionItemRequest> items;

    @Size(max = 2000)
    private String observation;

    @Size(max = 255)
    private String reference;

    @NotBlank
    @Size(max = 100)
    private String idempotencyKey;
}
