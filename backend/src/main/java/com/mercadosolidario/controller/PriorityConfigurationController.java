package com.mercadosolidario.controller;

import com.mercadosolidario.dto.priority.PriorityConfigurationRequest;
import com.mercadosolidario.dto.priority.PriorityConfigurationResponse;
import com.mercadosolidario.dto.priority.PriorityCalculationResponse;
import com.mercadosolidario.service.PriorityCalculationService;
import com.mercadosolidario.service.PriorityConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/priority/configuration")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PriorityConfigurationController {

    private final PriorityConfigurationService configurationService;
    private final PriorityCalculationService calculationService;

    @GetMapping
    public List<PriorityConfigurationResponse> list() {
        return configurationService.list();
    }

    @GetMapping("/history")
    public List<PriorityCalculationResponse> history() {
        return calculationService.getHistory();
    }

    @PostMapping
    public PriorityConfigurationResponse create(@Valid @RequestBody PriorityConfigurationRequest request) {
        return configurationService.create(request);
    }

    @PutMapping("/{id}")
    public PriorityConfigurationResponse createVersion(
            @PathVariable Long id,
            @Valid @RequestBody PriorityConfigurationRequest request) {
        return configurationService.createVersion(id, request);
    }

    @PostMapping("/{id}/activate")
    public PriorityConfigurationResponse activate(@PathVariable Long id) {
        return configurationService.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    public PriorityConfigurationResponse deactivate(@PathVariable Long id) {
        return configurationService.deactivate(id);
    }
}
