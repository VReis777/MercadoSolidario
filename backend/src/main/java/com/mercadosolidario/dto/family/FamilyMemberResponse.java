package com.mercadosolidario.dto.family;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberResponse {

    private Long id;
    private String name;
    private String cpf;
    private LocalDate birthDate;
    private String relationship;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
