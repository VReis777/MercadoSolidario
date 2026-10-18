package com.mercadosolidario.controller;

import com.mercadosolidario.dto.family.FamilyCreateRequest;
import com.mercadosolidario.dto.family.FamilyResponse;
import com.mercadosolidario.dto.family.FamilySearchRequest;
import com.mercadosolidario.dto.family.FamilyUpdateRequest;
import com.mercadosolidario.dto.distribution.DistributionResponse;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.service.DistributionService;
import com.mercadosolidario.service.FamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;
    private final DistributionService distributionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public ResponseEntity<FamilyResponse> create(@Valid @RequestBody FamilyCreateRequest request) {
        FamilyResponse response = familyService.create(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public ResponseEntity<Page<FamilyResponse>> search(
            FamilySearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FamilyResponse> response = familyService.search(request, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public ResponseEntity<FamilyResponse> findById(@PathVariable Long id) {
        FamilyResponse response = familyService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{familyId}/distributions")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public java.util.List<DistributionResponse> distributions(@PathVariable Long familyId) {
        return distributionService.byFamily(familyId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public ResponseEntity<FamilyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FamilyUpdateRequest request) {
        FamilyResponse response = familyService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public ResponseEntity<FamilyResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam FamilyStatus status) {
        FamilyResponse response = familyService.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }
}
