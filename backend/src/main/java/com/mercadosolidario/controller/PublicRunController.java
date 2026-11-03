package com.mercadosolidario.controller;

import com.mercadosolidario.dto.run.RunDtos.RunInfoResponse;
import com.mercadosolidario.dto.run.RunDtos.RunRegisterRequest;
import com.mercadosolidario.dto.run.RunDtos.RunRegistrationResponse;
import com.mercadosolidario.dto.run.RunDtos.RunStatusResponse;
import com.mercadosolidario.dto.run.RunDtos.RunVerifyResponse;
import com.mercadosolidario.service.RunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/rum")
@RequiredArgsConstructor
public class PublicRunController {

    private final RunService runService;

    @GetMapping
    public RunInfoResponse info() {
        return runService.info();
    }

    @PostMapping("/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public RunRegistrationResponse register(@Valid @RequestBody RunRegisterRequest request) {
        return runService.register(request);
    }

    @GetMapping("/registrations/{id}")
    public RunStatusResponse status(@PathVariable Long id) {
        return runService.status(id);
    }

    // Verificação pública pelo QR na retirada do kit.
    @GetMapping("/verify/{code}")
    public RunVerifyResponse verify(@PathVariable String code) {
        return runService.verify(code);
    }

    // Webhook do gateway de pagamento (Mercado Pago envia data.id ou ?id&topic).
    // A confirmação real é sempre consultada no gateway — nunca confiamos no corpo.
    @PostMapping("/webhook/{provider}")
    public Map<String, Boolean> webhook(@PathVariable String provider,
                                        @RequestBody(required = false) Map<String, Object> body,
                                        @RequestParam Map<String, String> params) {
        String paymentId = extractPaymentId(body, params);
        if ("mercadopago".equalsIgnoreCase(provider) && paymentId != null) {
            runService.confirmPayment(paymentId);
        }
        return Map.of("received", true);
    }

    private String extractPaymentId(Map<String, Object> body, Map<String, String> params) {
        // MP formato novo: body { "type": "payment", "data": { "id": "123" } }
        if (body != null) {
            Object data = body.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                Object id = dataMap.get("id");
                if (id != null) {
                    return id.toString();
                }
            }
            Object id = body.get("id");
            if (id != null && "payment".equals(String.valueOf(body.get("type")))) {
                return id.toString();
            }
        }
        // MP formato antigo: ?topic=payment&id=123
        if ("payment".equals(params.get("topic"))) {
            return params.get("id");
        }
        return null;
    }
}
