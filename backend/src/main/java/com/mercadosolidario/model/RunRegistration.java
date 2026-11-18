package com.mercadosolidario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(name = "birth_date")
    private LocalDate birthDate;

    // '3KM' | '7KM'
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

    // Taxa de R$7,00 cobrada só no cartão para cobrir a taxa do gateway.
    public static final BigDecimal CARD_SURCHARGE = new BigDecimal("7.00");

    /** Valor final cobrado do inscrito: inscrição + extra + taxa de cartão (se aplicável). */
    public BigDecimal amountDue() {
        return totalCharged().add("CARTAO".equals(paymentMethod) ? CARD_SURCHARGE : BigDecimal.ZERO);
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

    // Valor líquido efetivamente recebido (após taxas do gateway); nulo = usar totalCharged.
    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;

    // Código único do QR de verificação (retirada do kit).
    @Column(name = "checkin_code", length = 40, unique = true)
    private String checkinCode;

    // Segredo do inscrito para consultar o próprio status (anti-enumeração).
    @Column(name = "access_key", length = 40, unique = true)
    private String accessKey;

    @Column(name = "kit_delivered_at")
    private LocalDateTime kitDeliveredAt;

    @Column(name = "kit_delivered_by", length = 160)
    private String kitDeliveredBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
