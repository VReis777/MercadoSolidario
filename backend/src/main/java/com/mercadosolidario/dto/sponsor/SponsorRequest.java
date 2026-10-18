package com.mercadosolidario.dto.sponsor;

import com.mercadosolidario.model.enums.SponsorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255)
    private String name;

    @NotNull(message = "Tipo é obrigatório")
    private SponsorType type;

    @Size(max = 255)
    private String companyName;

    @Size(max = 20)
    private String phone;

    @Size(max = 20)
    private String whatsapp;

    @Size(max = 255)
    private String email;

    private String observations;

    private Boolean active;
}
