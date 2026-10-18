package com.mercadosolidario.dto.family;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberRequest {

    @NotBlank(message = "Nome do membro é obrigatório")
    @Size(max = 255, message = "Nome do membro deve ter no máximo 255 caracteres")
    private String name;

    @Pattern(regexp = "^\\d{11}$|^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$", message = "CPF deve ter formato válido")
    private String cpf;

    private LocalDate birthDate;

    @Size(max = 100, message = "Parentesco deve ter no máximo 100 caracteres")
    private String relationship;

    private String observations;
}
