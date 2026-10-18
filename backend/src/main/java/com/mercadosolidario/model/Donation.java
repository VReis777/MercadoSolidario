package com.mercadosolidario.model;

import com.mercadosolidario.model.enums.DonationStatus;
import com.mercadosolidario.model.enums.DonationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "donation_type", nullable = false, length = 20)
    private DonationType type = DonationType.FINANCEIRA;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "donor_name", length = 255)
    private String donorName;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DonationStatus status = DonationStatus.CRIADA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id")
    private Sponsor sponsor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @Column(name = "external_reference", length = 100, unique = true)
    private String externalReference;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "pix_payload", columnDefinition = "TEXT")
    private String pixPayload;

    @Column(name = "confirmation_date")
    private LocalDateTime confirmationDate;

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
        Donation donation = (Donation) o;
        return Objects.equals(id, donation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
