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

    /** Cartão para doação avulsa: external_reference "DON-<id>"; até 2x a partir de R$250. */
    Optional<Charge> createDonationCardCheckout(Long donationId, BigDecimal amount, String payerName);

    /** PIX para doação avulsa: external_reference "DON-<id>"; confirmação automática via webhook. */
    Optional<Charge> createDonationPixCharge(Long donationId, BigDecimal amount, String payerName);

    /**
     * Se o pagamento estiver aprovado, retorna a external_reference
     * (id da inscrição) enviada na criação da cobrança.
     */
    Optional<String> approvedExternalReference(String paymentRef);

    /**
     * true se já existe pagamento aprovado no gateway com esta
     * external_reference — usado na confirmação sob demanda quando
     * o webhook não chega.
     */
    boolean hasApprovedPaymentFor(String externalReference);

    /**
     * Valor líquido recebido (após taxas) de um pagamento aprovado,
     * consultado pelo id do pagamento. Vazio se indisponível.
     */
    default Optional<BigDecimal> approvedNetReceived(String paymentId) {
        return Optional.empty();
    }

    /**
     * Valor líquido recebido do pagamento aprovado com esta
     * external_reference (funciona para checkout e PIX do gateway).
     */
    default Optional<BigDecimal> approvedNetReceivedForReference(String externalReference) {
        return Optional.empty();
    }

    /**
     * Forma real de pagamento do pagamento aprovado com esta
     * external_reference: "PIX" ou "CARTAO" — cobre quem troca de
     * método dentro do checkout (ex.: inscreveu cartão, pagou PIX).
     */
    default Optional<String> approvedPaymentMethodForReference(String externalReference) {
        return Optional.empty();
    }

    /** Pagamento aprovado encontrado pelo external_reference, com dados para reatribuição. amount = valor bruto cobrado. */
    record ApprovedPaymentInfo(String paymentId, BigDecimal netAmount, String paymentMethod,
                               BigDecimal amount, String externalReference) {}

    /**
     * Detalhes do pagamento aprovado com esta external_reference —
     * usado para vincular um pagamento à inscrição correta quando a
     * pessoa pagou pelo link/código de outra inscrição.
     */
    default Optional<ApprovedPaymentInfo> approvedPaymentInfoForReference(String externalReference) {
        return Optional.empty();
    }

    record Charge(String paymentRef, String pixCode, String checkoutUrl) {}

    static BigDecimal reais(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2);
    }
}
