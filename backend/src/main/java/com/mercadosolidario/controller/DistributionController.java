package com.mercadosolidario.controller;

import com.mercadosolidario.dto.distribution.CancelDistributionRequest;
import com.mercadosolidario.dto.distribution.CreateDistributionRequest;
import com.mercadosolidario.dto.distribution.DistributionResponse;
import com.mercadosolidario.service.DistributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distributions")
@RequiredArgsConstructor
public class DistributionController {

    private final DistributionService distributionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public List<DistributionResponse> list() {
        return distributionService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public DistributionResponse get(@PathVariable Long id) {
        return distributionService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public DistributionResponse create(@Valid @RequestBody CreateDistributionRequest request) {
        return distributionService.create(request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public DistributionResponse cancel(
            @PathVariable Long id, @Valid @RequestBody CancelDistributionRequest request) {
        return distributionService.cancel(id, request);
    }
}
