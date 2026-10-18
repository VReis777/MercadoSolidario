package com.mercadosolidario.controller;

import com.mercadosolidario.dto.servicerequest.*;
import com.mercadosolidario.model.ServiceRequestStatus;
import com.mercadosolidario.service.ServiceRequestAdministrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
public class ServiceRequestAdministrationController {
    private final ServiceRequestAdministrationService service;

    @GetMapping
    public Page<ServiceRequestListResponse> list(
            @RequestParam(required = false) ServiceRequestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String protocol,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return service.list(status, fromDate, toDate, protocol, pageable, authentication);
    }

    @GetMapping("/{id}")
    public ServiceRequestResponse get(@PathVariable Long id, Authentication authentication) {
        return service.get(id, authentication);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public ServiceRequestResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequestStatusRequest request,
            Authentication authentication) {
        return service.changeStatus(id, request.newStatus(), request.observation(), authentication);
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceRequestResponse addNote(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequestNoteRequest request,
            Authentication authentication) {
        return service.addNote(id, request.observation(), authentication);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public ServiceRequestResponse assign(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequestAssignmentRequest request,
            Authentication authentication) {
        return service.assign(id, request.username(), request.observation(), authentication);
    }

    @GetMapping("/{id}/history")
    public List<ServiceRequestHistoryResponse> history(@PathVariable Long id, Authentication authentication) {
        return service.getHistory(id, authentication);
    }
}
