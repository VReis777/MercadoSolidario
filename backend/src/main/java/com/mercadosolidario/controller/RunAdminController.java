package com.mercadosolidario.controller;

import com.mercadosolidario.dto.run.RunDtos.AdminRunRegistrationResponse;
import com.mercadosolidario.dto.run.RunDtos.UpdateRunRegistrationRequest;
import com.mercadosolidario.dto.run.RunDtos.RunStatusResponse;
import com.mercadosolidario.dto.run.RunDtos.RunVerifyResponse;
import com.mercadosolidario.service.RunService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public AdminRunRegistrationResponse update(@PathVariable Long id,
                                               @RequestBody UpdateRunRegistrationRequest request) {
        return runService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        runService.delete(id);
    }

    @PostMapping("/{id}/send-retry-email")
    @PreAuthorize("hasRole('ADMIN')")
    public void sendRetryEmail(@PathVariable Long id) {
        runService.sendRetryEmail(id);
    }

    @PostMapping("/{id}/payment-link")
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.Map<String, String> paymentLink(@PathVariable Long id) {
        return java.util.Map.of("checkoutUrl", runService.paymentLink(id));
    }

    @PostMapping("/{id}/revert-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public RunStatusResponse revertPayment(@PathVariable Long id) {
        return runService.revertPayment(id);
    }

    @PostMapping("/{id}/sync-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminRunRegistrationResponse syncPayment(@PathVariable Long id) {
        return runService.syncPayment(id);
    }

    @PostMapping("/{id}/claim-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminRunRegistrationResponse claimPayment(@PathVariable Long id,
                                                     @RequestBody java.util.Map<String, Long> body) {
        return runService.claimPayment(id, body.get("fromId"));
    }

    @PostMapping("/{id}/recadastrar")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminRunRegistrationResponse recadastrar(@PathVariable Long id) {
        return runService.recadastrar(id);
    }

    @PostMapping("/{id}/resend-qr-email")
    @PreAuthorize("hasRole('ADMIN')")
    public void resendQrEmail(@PathVariable Long id) {
        runService.resendQrEmail(id);
    }

    // Retirada do kit pelo QR — equipe no local (ADMIN, RH ou voluntário).
    @PostMapping("/kit/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public RunVerifyResponse kitDelivered(@PathVariable String code) {
        return runService.markKitDelivered(code);
    }
}
