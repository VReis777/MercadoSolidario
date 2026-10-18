package com.mercadosolidario.dto.distribution;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DistributionItemRequest {

    @NotNull
    private Long productId;

    @NotNull
    @Positive(message = "A quantidade deve ser maior que zero")
    private Integer quantity;
}
