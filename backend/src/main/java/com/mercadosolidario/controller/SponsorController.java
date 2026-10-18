package com.mercadosolidario.controller;

import com.mercadosolidario.dto.sponsor.SponsorRequest;
import com.mercadosolidario.dto.sponsor.SponsorResponse;
import com.mercadosolidario.service.SponsorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sponsors")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public List<SponsorResponse> list(@RequestParam(required = false) String search,
                                      @RequestParam(defaultValue = "false") boolean onlyActive) {
        return sponsorService.list(search, onlyActive);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public SponsorResponse create(@Valid @RequestBody SponsorRequest request) {
        return sponsorService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public SponsorResponse update(@PathVariable Long id, @Valid @RequestBody SponsorRequest request) {
        return sponsorService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public SponsorResponse setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return sponsorService.setActive(id, Boolean.TRUE.equals(body.get("active")));
    }
}
