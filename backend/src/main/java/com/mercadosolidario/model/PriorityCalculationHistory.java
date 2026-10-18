package com.mercadosolidario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "priority_calculation_history")
@Getter
@Setter
@NoArgsConstructor
public class PriorityCalculationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuration_id", nullable = false)
    private PriorityConfiguration configuration;

    @Column(name = "score", nullable = false, precision = 12, scale = 2)
    private BigDecimal score;

    @Column(name = "priority_position")
    private Integer priorityPosition;

    @Column(name = "calculation_version", nullable = false, length = 100)
    private String calculationVersion;

    @Column(name = "factor_snapshot", nullable = false, columnDefinition = "TEXT")
    private String factorSnapshot;

    @Column(name = "calculated_by", nullable = false, length = 100)
    private String calculatedBy;

    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriorityCalculationHistory that = (PriorityCalculationHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
