package com.mercadosolidario.controller;

import com.mercadosolidario.dto.priority.PriorityCalculationResponse;
import com.mercadosolidario.dto.priority.PriorityRankingResponse;
import com.mercadosolidario.service.PriorityCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
public class FamilyPriorityController {

    private final PriorityCalculationService calculationService;

    @GetMapping("/priority")
    public PriorityRankingResponse ranking() {
        return calculationService.getRanking();
    }

    @PostMapping("/priority/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    public PriorityRankingResponse recalculateRanking() {
        return calculationService.calculateRanking();
    }

    @GetMapping("/{id}/priority")
    public PriorityCalculationResponse calculate(@PathVariable Long id) {
        return calculationService.getFamilyPriority(id);
    }
}
