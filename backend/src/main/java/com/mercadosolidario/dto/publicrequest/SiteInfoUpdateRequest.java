package com.mercadosolidario.dto.publicrequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SiteInfoUpdateRequest {

    @Size(max = 500)
    private String whatsappGroupUrl;

    @Size(max = 500)
    private String instagramUrl;
}
