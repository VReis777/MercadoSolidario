package com.mercadosolidario.model;

import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.model.enums.RequestReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "families")
@Getter
@Setter
@NoArgsConstructor
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "responsible_name", nullable = false, length = 255)
    private String responsibleName;

    @Column(name = "cpf", nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(name = "spouse_name", length = 255)
    private String spouseName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "dependents")
    private Integer dependents = 0;

    @Column(name = "household_size", nullable = false)
    private Integer householdSize;

    @Column(name = "total_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "pays_rent")
    private Boolean paysRent = false;

    @Column(name = "rent_value", precision = 12, scale = 2)
    private BigDecimal rentValue;

    @Column(name = "disabled_count")
    private Integer disabledCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_reason", nullable = false)
    private RequestReason requestReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    private FamilyOrigin origin;

    @Column(name = "referrer_name", length = 255)
    private String referrerName;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "previous_beneficiary")
    private Boolean previousBeneficiary = false;

    @Column(name = "previous_beneficiary_years", length = 255)
    private String previousBeneficiaryYears;

    @Column(name = "previous_beneficiary_notes", columnDefinition = "TEXT")
    private String previousBeneficiaryNotes;

    @Column(name = "priority_adjustment", precision = 12, scale = 2)
    private BigDecimal priorityAdjustment = BigDecimal.ZERO;

    @Column(name = "priority_adjustment_note", columnDefinition = "TEXT")
    private String priorityAdjustmentNote;

    @Column(name = "priority_adjusted_by", length = 100)
    private String priorityAdjustedBy;

    @Column(name = "priority_adjusted_at")
    private LocalDateTime priorityAdjustedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FamilyStatus status = FamilyStatus.CADASTRADA;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<FamilyMember> members = new HashSet<>();

    @OneToOne(mappedBy = "family", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private FamilyPriorityScore priorityScore;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Family family = (Family) o;
        return Objects.equals(id, family.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
