package com.mercadosolidario.dto.family;

import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.RequestReason;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyCreateRequest {

    @NotBlank(message = "Nome do responsável é obrigatório")
    @Size(max = 255, message = "Nome do responsável deve ter no máximo 255 caracteres")
    private String responsibleName;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "^\\d{11}$|^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$", message = "CPF deve ter formato válido")
    private String cpf;

    @Size(max = 255, message = "Nome do cônjuge deve ter no máximo 255 caracteres")
    private String spouseName;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String phone;

    @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
    private String address;

    @NotNull(message = "Quantidade de pessoas na residência é obrigatória")
    @Min(value = 1, message = "Quantidade de pessoas deve ser pelo menos 1")
    private Integer householdSize;

    @NotNull(message = "Renda familiar é obrigatória")
    @DecimalMin(value = "0.00", message = "Renda familiar não pode ser negativa")
    private BigDecimal totalIncome;

    @NotNull(message = "Motivo da solicitação é obrigatório")
    private RequestReason requestReason;

    @NotNull(message = "Origem é obrigatória")
    private FamilyOrigin origin;

    @Size(max = 255, message = "Nome do indicador deve ter no máximo 255 caracteres")
    private String referrerName;

    private String observations;

    private List<FamilyMemberRequest> members;
}
