package com.mercadosolidario.dto.donation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProductDonationRequest {

    @NotNull
    private Long productId;

    @NotNull
    @Positive(message = "A quantidade deve ser maior que zero")
    private Integer quantity;

    @Size(max = 255)
    private String donorName;

    private boolean anonymous;

    @Size(max = 2000)
    private String observation;

    @NotBlank
    @Size(max = 100)
    private String externalReference;
}
