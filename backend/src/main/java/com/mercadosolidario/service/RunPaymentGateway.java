package com.mercadosolidario.service;

import com.mercadosolidario.model.RunRegistration;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Abstração do gateway de pagamento da corrida. Implementações concretas:
 * Mercado Pago (ativado via APP_PAYMENT_MP_TOKEN) e, no futuro, Asaas.
 * Se nenhum gateway estiver configurado, o PIX cai na confirmação manual
 * (copia e cola já existente) e cartão fica indisponível.
 */
public interface RunPaymentGateway {

    boolean isEnabled();

    /** PIX automático: retorna o código copia-e-cola (EMV) e o id do pagamento. */
    Optional<Charge> createPixCharge(RunRegistration registration);

    /** Cartão: retorna a URL de checkout hospedada no gateway e o id da cobrança. */
    Optional<Charge> createCardCheckout(RunRegistration registration);

    /**
     * Se o pagamento estiver aprovado, retorna a external_reference
     * (id da inscrição) enviada na criação da cobrança.
     */
    Optional<String> approvedExternalReference(String paymentRef);

    record Charge(String paymentRef, String pixCode, String checkoutUrl) {}

    static BigDecimal reais(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2);
    }
}
