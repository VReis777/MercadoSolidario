package com.mercadosolidario.dto.publicrequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceRequestLookupRequest(
    @NotNull @Size(max = 128) String protocol,
    @NotNull @Size(max = 254) String contactValue
) {
}
