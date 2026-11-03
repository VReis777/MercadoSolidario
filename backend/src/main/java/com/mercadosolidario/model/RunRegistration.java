package com.mercadosolidario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "run_registrations")
@Getter
@Setter
public class RunRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    // '3KM' | '6KM'
    @Column(name = "modality", nullable = false, length = 10)
    private String modality;

    @Column(name = "shirt_size", length = 10)
    private String shirtSize;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Upsell: doação extra opcional cobrada junto com a inscrição.
    @Column(name = "extra_donation", precision = 12, scale = 2)
    private BigDecimal extraDonation;

    public BigDecimal totalCharged() {
        return amount.add(extraDonation != null ? extraDonation : BigDecimal.ZERO);
    }

    // 'PIX' | 'CARTAO'
    @Column(name = "payment_method", nullable = false, length = 10)
    private String paymentMethod;

    // id do pagamento no gateway (Mercado Pago, Asaas...)
    @Column(name = "payment_ref", length = 80)
    private String paymentRef;

    // PENDENTE | PAGA | CANCELADA
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDENTE";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
