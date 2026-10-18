package com.mercadosolidario.dto.donation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateFinancialDonationRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "O valor da doação deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "Informe um valor com no máximo duas casas decimais")
    private BigDecimal amount;

    @Size(max = 255)
    private String donorName;

    private boolean anonymous;

    @NotBlank
    @Size(max = 100)
    private String externalReference;
}
