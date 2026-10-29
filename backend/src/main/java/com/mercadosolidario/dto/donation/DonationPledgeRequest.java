package com.mercadosolidario.dto.donation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// packages = quantidade de pacotes da embalagem do item (ex.: 2 pacotes de 500 g).
// Contato obrigatório quando selfDelivery=false (precisamos combinar a retirada).
public record DonationPledgeRequest(
    @NotNull Long goalItemId,
    @NotNull @Min(1) Integer packages,
    @Size(max = 255) String donorName,
    @Size(max = 160) String donorContact,
    boolean anonymous,
    boolean selfDelivery
) {}
