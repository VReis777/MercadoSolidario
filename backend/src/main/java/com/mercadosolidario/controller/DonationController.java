package com.mercadosolidario.controller;

import com.mercadosolidario.dto.donation.*;
import com.mercadosolidario.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    public PublicDonationResponse createFinancial(@Valid @RequestBody CreateFinancialDonationRequest request) {
        return donationService.createFinancial(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public List<DonationResponse> list() {
        return donationService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public DonationResponse get(@PathVariable Long id) {
        return donationService.get(id);
    }

    @PostMapping("/{id}/confirm-manually")
    @PreAuthorize("hasRole('ADMIN')")
    public DonationResponse confirmManually(@PathVariable Long id) {
        return donationService.confirmManually(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public DonationResponse cancel(@PathVariable Long id) {
        return donationService.cancel(id);
    }

    @PostMapping("/{id}/expire")
    @PreAuthorize("hasRole('ADMIN')")
    public DonationResponse expire(@PathVariable Long id) {
        return donationService.expire(id);
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public DonationResponse createProductDonation(
            @Valid @RequestBody CreateProductDonationRequest request) {
        return donationService.createProductDonation(request);
    }

    @GetMapping("/configuration")
    @PreAuthorize("hasRole('ADMIN')")
    public PixConfigurationResponse getPixConfiguration() {
        return donationService.getPixConfiguration();
    }

    @PutMapping("/configuration")
    @PreAuthorize("hasRole('ADMIN')")
    public PixConfigurationResponse updatePixConfiguration(
            @Valid @RequestBody PixConfigurationRequest request) {
        return donationService.updatePixConfiguration(request);
    }
}
