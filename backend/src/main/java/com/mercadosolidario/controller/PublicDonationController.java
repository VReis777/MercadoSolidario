package com.mercadosolidario.controller;

import com.mercadosolidario.dto.donation.PublicPixInfoResponse;
import com.mercadosolidario.dto.family.FamilyCreateRequest;
import com.mercadosolidario.dto.family.FamilyResponse;
import com.mercadosolidario.dto.publicrequest.CompanyContactRequest;
import com.mercadosolidario.dto.publicrequest.SiteInfoResponse;
import com.mercadosolidario.service.DonationService;
import com.mercadosolidario.service.FamilyService;
import com.mercadosolidario.service.PublicCompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicDonationController {

    private final DonationService donationService;
    private final FamilyService familyService;
    private final PublicCompanyService companyService;

    @GetMapping("/donations/pix-info")
    public PublicPixInfoResponse pixInfo() {
        return donationService.getPublicPixInfo();
    }

    @GetMapping("/site-info")
    public SiteInfoResponse siteInfo() {
        return companyService.siteInfo();
    }

    @PostMapping("/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registerCompany(@Valid @RequestBody CompanyContactRequest request) {
        boolean emailSent = companyService.registerContact(request);
        return Map.of(
                "registered", true,
                "emailSent", emailSent,
                "message", emailSent
                        ? "Recebemos seu contato! Nossa equipe foi avisada por e-mail e vai retornar em breve."
                        : "Recebemos seu contato! Ele já está na nossa lista de empresas.");
    }

    @PostMapping("/beneficiaries")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registerBeneficiary(@Valid @RequestBody FamilyCreateRequest request) {
        FamilyResponse created = familyService.create(request);
        return Map.of(
                "registered", true,
                "familyId", created.getId(),
                "message", "Cadastro recebido. Suas informações foram registradas e serão analisadas pela nossa equipe.");
    }
}
