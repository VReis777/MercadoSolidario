package com.mercadosolidario.dto.sponsor;

import com.mercadosolidario.model.enums.SponsorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorResponse {

    private Long id;
    private String name;
    private SponsorType type;
    private String companyName;
    private String phone;
    private String whatsapp;
    private String email;
    private String observations;
    private Boolean active;
    private LocalDateTime createdAt;
}
