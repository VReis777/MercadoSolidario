package com.mercadosolidario.controller;

import com.mercadosolidario.dto.publicrequest.SiteInfoResponse;
import com.mercadosolidario.dto.publicrequest.SiteInfoUpdateRequest;
import com.mercadosolidario.service.PublicCompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/site-info")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSiteInfoController {

    private final PublicCompanyService companyService;

    @GetMapping
    public SiteInfoResponse get() {
        return companyService.siteInfo();
    }

    @PutMapping
    public SiteInfoResponse update(@Valid @RequestBody SiteInfoUpdateRequest request) {
        return companyService.updateSiteInfo(request);
    }
}
