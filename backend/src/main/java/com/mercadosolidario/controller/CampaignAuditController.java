package com.mercadosolidario.controller;

import com.mercadosolidario.dto.history.*;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.service.CampaignAuditService;
import com.mercadosolidario.service.HistoricalBeneficiaryImportService;
import com.mercadosolidario.service.RegistrationImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Year;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CampaignAuditController {

    private final CampaignAuditService auditService;
    private final HistoricalBeneficiaryImportService importService;
    private final RegistrationImportService registrationImportService;

    @GetMapping("/campaign/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public CampaignOverviewResponse overview(@RequestParam(required = false) Integer year) {
        return auditService.overview(year);
    }

    @GetMapping("/campaign/registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public Page<CampaignRegistrationResponse> registrations(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FamilyStatus status,
            @RequestParam(defaultValue = "false") boolean onlyWithHistory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditService.registrations(year, search, status, onlyWithHistory, page, size);
    }

    @GetMapping("/historical-beneficiaries")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public Page<HistoricalBeneficiaryResponse> history(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String origin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditService.searchHistory(year, search, origin, page, size);
    }

    @PostMapping(value = "/historical-beneficiaries/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public HistoricalImportPreview previewImport(@RequestParam("file") MultipartFile file, @RequestParam int year) {
        return importService.preview(file, year);
    }

    @PostMapping(value = "/historical-beneficiaries/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public HistoricalImportResult confirmImport(@RequestParam("file") MultipartFile file, @RequestParam int year) {
        return importService.confirm(file, year);
    }

    @GetMapping("/campaign/registrations/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA', 'DIACONO')")
    public ResponseEntity<byte[]> registrationTemplate() {
        int year = Year.now().getValue();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"modelo-cadastros-" + year + ".xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(registrationImportService.buildTemplate(year));
    }

    @PostMapping(value = "/campaign/registrations/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public RegistrationImportPreview previewRegistrationsImport(@RequestParam("file") MultipartFile file) {
        return registrationImportService.preview(file);
    }

    @PostMapping(value = "/campaign/registrations/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public RegistrationImportResult confirmRegistrationsImport(@RequestParam("file") MultipartFile file) {
        return registrationImportService.confirm(file);
    }
}
