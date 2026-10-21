package com.mercadosolidario.dto.family;

import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.model.enums.RequestReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyResponse {

    private Long id;
    private String responsibleName;
    private String cpf;
    private String spouseName;
    private String phone;
    private String address;
    private Integer dependents;
    private Integer householdSize;
    private BigDecimal totalIncome;
    private Boolean paysRent;
    private BigDecimal rentValue;
    private Integer disabledCount;
    private RequestReason requestReason;
    private FamilyOrigin origin;
    private String referrerName;
    private String observations;
    private Boolean previousBeneficiary;
    private String previousBeneficiaryYears;
    private String previousBeneficiaryNotes;
    private FamilyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FamilyMemberResponse> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FamilyMemberResponse {
        private Long id;
        private String name;
        private String cpf;
        private java.time.LocalDate birthDate;
        private String relationship;
        private String observations;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
