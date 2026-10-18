package com.mercadosolidario.controller;

import com.mercadosolidario.dto.publicrequest.CreatePublicServiceRequest;
import com.mercadosolidario.dto.publicrequest.PublicServiceRequestCreatedResponse;
import com.mercadosolidario.dto.publicrequest.ServiceRequestLookupRequest;
import com.mercadosolidario.dto.publicrequest.ServiceRequestLookupResponse;
import com.mercadosolidario.security.ServiceRequestLookupRateLimiter;
import com.mercadosolidario.service.PublicServiceRequestService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/service-requests")
@RequiredArgsConstructor
public class PublicServiceRequestController {

    private final PublicServiceRequestService service;
    private final ServiceRequestLookupRateLimiter rateLimiter;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicServiceRequestCreatedResponse create(@Valid @RequestBody CreatePublicServiceRequest request) {
        return service.create(request);
    }

    @PostMapping("/lookup")
    public ResponseEntity<?> lookup(
            @Valid @RequestBody ServiceRequestLookupRequest request,
            HttpServletRequest servletRequest) {
        if (!rateLimiter.allow(servletRequest)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "message", "Consulta temporariamente indisponível"
            ));
        }
        ServiceRequestLookupResponse response = service.lookup(request);
        return ResponseEntity.ok(response);
    }
}
