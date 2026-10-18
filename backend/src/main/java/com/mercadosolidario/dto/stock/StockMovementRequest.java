package com.mercadosolidario.dto.stock;

import com.mercadosolidario.model.enums.MovementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StockMovementRequest {

    @NotNull
    private Long productId;

    @NotNull
    private MovementType type;

    @NotNull
    @Positive(message = "A quantidade movimentada deve ser maior que zero")
    private Integer quantity;

    private AdjustmentDirection adjustmentDirection;

    @NotBlank(message = "O motivo da movimentação é obrigatório")
    @Size(max = 2000)
    private String reason;

    @Size(max = 2000)
    private String observation;

    public enum AdjustmentDirection {
        ENTRADA,
        SAIDA
    }
}
