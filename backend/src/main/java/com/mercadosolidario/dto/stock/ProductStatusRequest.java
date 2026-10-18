package com.mercadosolidario.dto.stock;

import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(@NotNull Boolean active) {}
