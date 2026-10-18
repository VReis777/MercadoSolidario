package com.mercadosolidario.dto.family;

import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.model.enums.RequestReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilySearchRequest {

    private String name;
    private String cpf;
    private Integer householdSize;
    private BigDecimal minIncome;
    private BigDecimal maxIncome;
    private FamilyOrigin origin;
    private RequestReason requestReason;
    private FamilyStatus status;
    private Boolean previousBeneficiary;
}
