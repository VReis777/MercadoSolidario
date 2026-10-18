package com.mercadosolidario.service;

import com.mercadosolidario.dto.sponsor.SponsorRequest;
import com.mercadosolidario.dto.sponsor.SponsorResponse;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.Sponsor;
import com.mercadosolidario.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SponsorService {

    private final SponsorRepository sponsorRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SponsorResponse> list(String search, Boolean onlyActive) {
        List<Sponsor> sponsors = search != null && !search.isBlank()
            ? sponsorRepository.findByNameContainingIgnoreCase(search.trim())
            : sponsorRepository.findAll();
        return sponsors.stream()
            .filter(s -> !Boolean.TRUE.equals(onlyActive) || Boolean.TRUE.equals(s.getActive()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SponsorResponse create(SponsorRequest request) {
        Sponsor sponsor = new Sponsor();
        apply(sponsor, request);
        sponsor.setActive(request.getActive() == null || request.getActive());
        sponsor = sponsorRepository.save(sponsor);
        auditService.log("SPONSOR_CREATED", "Sponsor", sponsor.getId(),
            "{\"name\": \"" + sponsor.getName().replace("\"", "") + "\"}");
        return toResponse(sponsor);
    }

    @Transactional
    public SponsorResponse update(Long id, SponsorRequest request) {
        Sponsor sponsor = sponsorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Patrocinador não encontrado"));
        apply(sponsor, request);
        if (request.getActive() != null) sponsor.setActive(request.getActive());
        sponsor = sponsorRepository.save(sponsor);
        auditService.log("SPONSOR_UPDATED", "Sponsor", sponsor.getId(),
            "{\"name\": \"" + sponsor.getName().replace("\"", "") + "\"}");
        return toResponse(sponsor);
    }

    @Transactional
    public SponsorResponse setActive(Long id, boolean active) {
        Sponsor sponsor = sponsorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Patrocinador não encontrado"));
        sponsor.setActive(active);
        sponsor = sponsorRepository.save(sponsor);
        auditService.log(active ? "SPONSOR_ACTIVATED" : "SPONSOR_DEACTIVATED", "Sponsor", id, null);
        return toResponse(sponsor);
    }

    private void apply(Sponsor sponsor, SponsorRequest request) {
        sponsor.setName(request.getName().trim());
        sponsor.setType(request.getType());
        sponsor.setCompanyName(request.getCompanyName());
        sponsor.setPhone(request.getPhone());
        sponsor.setWhatsapp(request.getWhatsapp());
        sponsor.setEmail(request.getEmail());
        sponsor.setObservations(request.getObservations());
    }

    private SponsorResponse toResponse(Sponsor s) {
        return SponsorResponse.builder()
            .id(s.getId())
            .name(s.getName())
            .type(s.getType())
            .companyName(s.getCompanyName())
            .phone(s.getPhone())
            .whatsapp(s.getWhatsapp())
            .email(s.getEmail())
            .observations(s.getObservations())
            .active(s.getActive())
            .createdAt(s.getCreatedAt())
            .build();
    }
}
