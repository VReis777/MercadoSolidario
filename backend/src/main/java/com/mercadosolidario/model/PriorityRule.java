package com.mercadosolidario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "priority_rules")
@Getter
@Setter
@NoArgsConstructor
public class PriorityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuration_id", nullable = false)
    private PriorityConfiguration configuration;

    @Column(name = "factor_code", nullable = false, length = 50)
    private String factorCode;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "operator", nullable = false, length = 20)
    private String operator;

    @Column(name = "reference_value", nullable = false, length = 255)
    private String referenceValue;

    @Column(name = "weight", nullable = false, precision = 12, scale = 2)
    private BigDecimal weight;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriorityRule that = (PriorityRule) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
