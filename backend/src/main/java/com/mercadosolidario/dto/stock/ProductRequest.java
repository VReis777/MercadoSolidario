package com.mercadosolidario.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductRequest {

    @NotBlank(message = "Nome do produto é obrigatório")
    @Size(max = 255)
    private String name;

    @Size(max = 100)
    private String category;

    @NotBlank(message = "Unidade de medida é obrigatória")
    @Size(max = 50)
    private String unit;

    @Min(value = 0, message = "Estoque mínimo não pode ser negativo")
    private Integer minimumQuantity = 0;

    @Min(value = 0, message = "Quantidade por família não pode ser negativa")
    private Integer basketQuantity;

    @Size(max = 500)
    private String notes;
}
