package com.mercadosolidario.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.model.RunRegistration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mercado Pago via REST (sem SDK): PIX gera cobrança com QR copia-e-cola;
 * cartão usa Checkout Pro (página hospedada do MP, sem PCI no nosso lado).
 * O gateway avisa o pagamento pelo webhook e confirmamos consultando o status.
 */
@Component
@Slf4j
public class MercadoPagoGateway implements RunPaymentGateway {

    private static final String API = "https://api.mercadopago.com";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${app.payment.mercadopago.token:}")
    private String accessToken;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.payment.notification-url:}")
    private String notificationUrl;

    @Override
    public boolean isEnabled() {
        // Credenciais de teste (TEST-*) não movimentam dinheiro real —
        // o gateway fica desabilitado e o site cai nos fluxos manuais.
        return accessToken != null && !accessToken.isBlank()
            && !accessToken.startsWith("TEST-");
    }

    @Override
    public Optional<Charge> createPixCharge(RunRegistration registration) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("transaction_amount", registration.totalCharged());
            body.put("description", "Inscrição Mercado Solidário Run " + registration.getModality());
            body.put("payment_method_id", "pix");
            body.put("external_reference", registration.getId().toString());
            putNotification(body);
            body.put("payer", Map.of(
                "email", registration.getEmail() != null && !registration.getEmail().isBlank()
                    ? registration.getEmail() : "inscricao@mercadosolidario.org",
                "first_name", registration.getFullName()));
            JsonNode response = post("/v1/payments", mapper.writeValueAsString(body));
            String ref = response.path("id").asText(null);
            String qr = response.path("point_of_interaction")
                .path("transaction_data").path("qr_code").asText(null);
            if (ref == null) {
                return Optional.empty();
            }
            return Optional.of(new Charge(ref, qr, null));
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao gerar cobrança PIX.", exception);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Charge> createCardCheckout(RunRegistration registration) {
        try {
            String inscriptionUrl = frontendBaseUrl + "/run/inscricao/" + registration.getId()
                + "?key=" + registration.getAccessKey();
            Map<String, Object> body = new HashMap<>();
            body.put("items", java.util.List.of(Map.of(
                "title", "Inscrição Mercado Solidário Run " + registration.getModality(),
                "quantity", 1,
                "currency_id", "BRL",
                "unit_price", registration.totalCharged())));
            body.put("external_reference", registration.getId().toString());
            putNotification(body);
            // Corrida: cartão somente à vista.
            body.put("payment_methods", Map.of("installments", 1));
            body.put("back_urls", Map.of(
                "success", inscriptionUrl,
                "failure", inscriptionUrl,
                "pending", inscriptionUrl));
            body.put("auto_return", "approved");
            JsonNode response = post("/checkout/preferences", mapper.writeValueAsString(body));
            String ref = response.path("id").asText(null);
            String url = response.path("init_point").asText(null);
            if (ref == null || url == null) {
                return Optional.empty();
            }
            return Optional.of(new Charge(ref, null, url));
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao criar checkout de cartão.", exception);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Charge> createDonationCardCheckout(Long donationId, BigDecimal amount, String payerName) {
        try {
            String donationUrl = frontendBaseUrl + "/doacao";
            // Doações a partir de R$250 podem parcelar em até 2x.
            int installments = amount.compareTo(new BigDecimal("250")) >= 0 ? 2 : 1;
            Map<String, Object> body = new HashMap<>();
            body.put("items", java.util.List.of(Map.of(
                "title", "Doação Mercado Solidário",
                "quantity", 1,
                "currency_id", "BRL",
                "unit_price", amount)));
            body.put("external_reference", "DON-" + donationId);
            putNotification(body);
            body.put("payment_methods", Map.of("installments", installments));
            body.put("payer", Map.of(
                "name", payerName != null && !payerName.isBlank() ? payerName : "Doador"));
            body.put("back_urls", Map.of(
                "success", donationUrl,
                "failure", donationUrl,
                "pending", donationUrl));
            body.put("auto_return", "approved");
            JsonNode response = post("/checkout/preferences", mapper.writeValueAsString(body));
            String ref = response.path("id").asText(null);
            String url = response.path("init_point").asText(null);
            if (ref == null || url == null) {
                return Optional.empty();
            }
            return Optional.of(new Charge(ref, null, url));
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao criar checkout de doação.", exception);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Charge> createDonationPixCharge(Long donationId, BigDecimal amount, String payerName) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("transaction_amount", amount);
            body.put("description", "Doação Mercado Solidário");
            body.put("payment_method_id", "pix");
            body.put("external_reference", "DON-" + donationId);
            putNotification(body);
            body.put("payer", Map.of(
                "email", "doacao@mercadosolidario.org",
                "first_name", payerName != null && !payerName.isBlank() ? payerName : "Doador"));
            JsonNode response = post("/v1/payments", mapper.writeValueAsString(body));
            String ref = response.path("id").asText(null);
            String qr = response.path("point_of_interaction")
                .path("transaction_data").path("qr_code").asText(null);
            if (ref == null || qr == null) {
                return Optional.empty();
            }
            return Optional.of(new Charge(ref, qr, null));
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao gerar cobrança PIX de doação.", exception);
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> approvedExternalReference(String paymentRef) {
        try {
            JsonNode node = getPayment(paymentRef);
            if (node == null || !"approved".equals(node.path("status").asText())) {
                return Optional.empty();
            }
            String external = node.path("external_reference").asText(null);
            return Optional.ofNullable(external);
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao consultar pagamento {}.", paymentRef);
            return Optional.empty();
        }
    }

    @Override
    public Optional<BigDecimal> approvedNetReceived(String paymentId) {
        try {
            JsonNode node = getPayment(paymentId);
            if (node == null || !"approved".equals(node.path("status").asText())) {
                return Optional.empty();
            }
            return netReceived(node);
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao consultar líquido do pagamento {}.", paymentId);
            return Optional.empty();
        }
    }

    @Override
    public Optional<BigDecimal> approvedNetReceivedForReference(String externalReference) {
        try {
            String url = API + "/v1/payments/search?external_reference="
                + URLEncoder.encode(externalReference, StandardCharsets.UTF_8) + "&limit=10";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofSeconds(15))
                .GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            for (JsonNode payment : mapper.readTree(response.body()).path("results")) {
                if ("approved".equals(payment.path("status").asText())
                        && externalReference.equals(payment.path("external_reference").asText(null))) {
                    return netReceived(payment);
                }
            }
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao buscar líquido por referência {}.", externalReference);
            return Optional.empty();
        }
    }

    private JsonNode getPayment(String paymentId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API + "/v1/payments/" + paymentId))
            .header("Authorization", "Bearer " + accessToken)
            .timeout(Duration.ofSeconds(15))
            .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            return null;
        }
        return mapper.readTree(response.body());
    }

    private Optional<BigDecimal> netReceived(JsonNode payment) {
        JsonNode net = payment.path("transaction_details").path("net_received_amount");
        if (net.isNumber()) {
            return Optional.of(net.decimalValue());
        }
        return Optional.empty();
    }

    /**
     * Confirmação sob demanda: busca no Mercado Pago se já existe pagamento
     * aprovado com esta external_reference. Cobre cartão (Checkout Pro, cujo
     * paymentRef é o id da preferência, não do pagamento) e funciona mesmo se
     * o webhook não estiver configurado.
     */
    @Override
    public boolean hasApprovedPaymentFor(String externalReference) {
        try {
            String url = API + "/v1/payments/search?external_reference="
                + URLEncoder.encode(externalReference, StandardCharsets.UTF_8) + "&limit=10";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofSeconds(15))
                .GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return false;
            }
            JsonNode results = mapper.readTree(response.body()).path("results");
            for (JsonNode payment : results) {
                if ("approved".equals(payment.path("status").asText())
                        && externalReference.equals(payment.path("external_reference").asText(null))) {
                    return true;
                }
            }
            return false;
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao buscar pagamento por referência {}.", externalReference);
            return false;
        }
    }

    /**
     * notification_url só é enviada quando é uma URL https válida —
     * vazia ou inválida faz o MP rejeitar a cobrança PIX.
     */
    private void putNotification(Map<String, Object> body) {
        if (notificationUrl != null && notificationUrl.startsWith("https://")) {
            body.put("notification_url", notificationUrl);
        }
    }

    private JsonNode post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API + path))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 300) {
            String detail = response.body() == null ? "" : response.body();
            if (detail.length() > 500) {
                detail = detail.substring(0, 500);
            }
            throw new IllegalStateException("Mercado Pago respondeu " + response.statusCode() + ": " + detail);
        }
        return mapper.readTree(response.body());
    }
}
