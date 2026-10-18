package com.mercadosolidario.dto.priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriorityFactorRequest {

    @NotBlank
    @Size(max = 50)
    private String factorCode;

    @NotBlank
    @Size(max = 150)
    private String label;

    @NotBlank
    @Size(max = 20)
    private String operator;

    @NotBlank
    @Size(max = 255)
    private String referenceValue;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    private BigDecimal weight;

    private boolean enabled;

    @NotBlank
    @Size(max = 2000)
    private String explanation;
}
