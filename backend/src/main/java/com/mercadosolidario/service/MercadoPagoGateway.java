package com.mercadosolidario.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.model.RunRegistration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
        return accessToken != null && !accessToken.isBlank();
    }

    @Override
    public Optional<Charge> createPixCharge(RunRegistration registration) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                "transaction_amount", registration.totalCharged(),
                "description", "Inscrição Mercado Solidário Rum " + registration.getModality(),
                "payment_method_id", "pix",
                "external_reference", registration.getId().toString(),
                "notification_url", notificationUrl,
                "payer", Map.of(
                    "email", registration.getEmail() != null && !registration.getEmail().isBlank()
                        ? registration.getEmail() : "inscricao@mercadosolidario.org",
                    "first_name", registration.getFullName())));
            JsonNode response = post("/v1/payments", body);
            String ref = response.path("id").asText(null);
            String qr = response.path("point_of_interaction")
                .path("transaction_data").path("qr_code").asText(null);
            if (ref == null) {
                return Optional.empty();
            }
            return Optional.of(new Charge(ref, qr, null));
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao gerar cobrança PIX.");
            return Optional.empty();
        }
    }

    @Override
    public Optional<Charge> createCardCheckout(RunRegistration registration) {
        try {
            String inscriptionUrl = frontendBaseUrl + "/rum/inscricao/" + registration.getId();
            String body = mapper.writeValueAsString(Map.of(
                "items", java.util.List.of(Map.of(
                    "title", "Inscrição Mercado Solidário Rum " + registration.getModality(),
                    "quantity", 1,
                    "currency_id", "BRL",
                    "unit_price", registration.totalCharged())),
                "external_reference", registration.getId().toString(),
                "notification_url", notificationUrl,
                "back_urls", Map.of(
                    "success", inscriptionUrl,
                    "failure", inscriptionUrl,
                    "pending", inscriptionUrl),
                "auto_return", "approved"));
            JsonNode response = post("/checkout/preferences", body);
            String ref = response.path("id").asText(null);
            String url = response.path("init_point").asText(null);
            if (ref == null || url == null) {
                return Optional.empty();
            }
            return Optional.of(new Charge(ref, null, url));
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao criar checkout de cartão.");
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> approvedExternalReference(String paymentRef) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API + "/v1/payments/" + paymentRef))
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofSeconds(15))
                .GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode node = mapper.readTree(response.body());
            if (!"approved".equals(node.path("status").asText())) {
                return Optional.empty();
            }
            String external = node.path("external_reference").asText(null);
            return Optional.ofNullable(external);
        } catch (Exception exception) {
            log.warn("Mercado Pago: falha ao consultar pagamento {}.", paymentRef);
            return Optional.empty();
        }
    }

    private JsonNode post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API + path))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Mercado Pago respondeu " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }
}
