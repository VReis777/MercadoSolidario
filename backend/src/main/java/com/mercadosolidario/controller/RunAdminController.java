package com.mercadosolidario.controller;

import com.mercadosolidario.dto.run.RunDtos.AdminRunRegistrationResponse;
import com.mercadosolidario.dto.run.RunDtos.RunStatusResponse;
import com.mercadosolidario.dto.run.RunDtos.RunVerifyResponse;
import com.mercadosolidario.service.RunService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rum/registrations")
@RequiredArgsConstructor
public class RunAdminController {

    private final RunService runService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public List<AdminRunRegistrationResponse> list() {
        return runService.listAll();
    }

    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public RunStatusResponse confirmPayment(@PathVariable Long id) {
        return runService.markPaidManually(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public RunStatusResponse cancel(@PathVariable Long id) {
        return runService.cancel(id);
    }

    // Retirada do kit pelo QR — equipe no local (ADMIN, RH ou voluntário).
    @PostMapping("/kit/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public RunVerifyResponse kitDelivered(@PathVariable String code) {
        return runService.markKitDelivered(code);
    }
}
