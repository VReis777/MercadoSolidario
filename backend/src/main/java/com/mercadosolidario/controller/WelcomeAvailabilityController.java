package com.mercadosolidario.controller;

import com.mercadosolidario.dto.servicerequest.WelcomeSlotRequest;
import com.mercadosolidario.dto.servicerequest.WelcomeSlotResponse;
import com.mercadosolidario.service.WelcomeAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WelcomeAvailabilityController {

    private final WelcomeAvailabilityService service;

    /** Datas oferecidas no formulário público de pedido de atendimento. */
    @GetMapping("/api/public/welcome-slots")
    public List<WelcomeSlotResponse> publicSlots() {
        return service.publicSlots();
    }

    @GetMapping("/api/service-requests/welcome-slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public List<WelcomeSlotResponse> listAll() {
        return service.listAll();
    }

    @PostMapping("/api/service-requests/welcome-slots")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public WelcomeSlotResponse create(@Valid @RequestBody WelcomeSlotRequest request) {
        return service.create(request);
    }

    @PatchMapping("/api/service-requests/welcome-slots/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public WelcomeSlotResponse setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return service.setActive(id, Boolean.TRUE.equals(body.get("active")));
    }
}
