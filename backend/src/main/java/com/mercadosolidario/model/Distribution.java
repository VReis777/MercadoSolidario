package com.mercadosolidario.model;

import jakarta.persistence.*;
import com.mercadosolidario.model.enums.DistributionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "distributions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_distributions_idempotency_key",
        columnNames = "idempotency_key"
    )
)
@Getter
@Setter
@NoArgsConstructor
public class Distribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity")
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "distribution_date")
    private LocalDateTime distributionDate;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DistributionStatus status = DistributionStatus.CONFIRMADA;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @OneToMany(mappedBy = "distribution", fetch = FetchType.LAZY)
    private List<DistributionItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (distributionDate == null) {
            distributionDate = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Distribution that = (Distribution) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
