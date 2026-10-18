package com.mercadosolidario.dto.donation;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PixConfigurationRequest {

    @Size(max = 200)
    private String pixKey;

    @Size(max = 25)
    private String merchantName;

    @Size(max = 15)
    private String merchantCity;

    private boolean clearPixKey;
}
