package com.mercadosolidario.service;

import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.model.enums.RequestReason;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.util.CpfValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreviousBeneficiaryImportService {

    private final FamilyRepository familyRepository;

    @Transactional
    public ImportResult importFromCsv(MultipartFile file, String year) throws Exception {
        int imported = 0;
        int updated = 0;
        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip header if present
                if (lineNumber == 1 && (line.toLowerCase().contains("nome") || line.toLowerCase().contains("cpf"))) {
                    continue;
                }
                
                String[] columns = line.split(";");
                if (columns.length < 2) {
                    errors++;
                    errorMessages.add("Linha " + lineNumber + ": Formato inválido - esperado pelo menos 2 colunas separadas por ;");
                    continue;
                }
                
                try {
                    String responsibleName = columns[0].trim();
                    String cpf = columns[1].trim();
                    String spouseName = columns.length > 2 ? columns[2].trim() : "";
                    String householdSizeStr = columns.length > 3 ? columns[3].trim() : "1";
                    String totalIncomeStr = columns.length > 4 ? columns[4].trim() : "0";
                    String origin = columns.length > 5 ? columns[5].trim() : "CADASTRO_PROPRIO";
                    String requestReason = columns.length > 6 ? columns[6].trim() : "BAIXA_RENDA";
                    String referrerName = columns.length > 7 ? columns[7].trim() : "";
                    String observations = columns.length > 8 ? columns[8].trim() : "";

                    if (responsibleName.isEmpty()) {
                        continue;
                    }

                    String normalizedCpf = CpfValidator.unformat(cpf);
                    if (normalizedCpf == null || normalizedCpf.isEmpty()) {
                        errors++;
                        errorMessages.add("Linha " + lineNumber + ": CPF não informado");
                        continue;
                    }

                    if (!CpfValidator.isValid(normalizedCpf)) {
                        errors++;
                        errorMessages.add("Linha " + lineNumber + ": CPF inválido - " + cpf);
                        continue;
                    }

                    Family existingFamily = familyRepository.findByCpf(normalizedCpf).orElse(null);

                    if (existingFamily != null) {
                        // Update existing family
                        existingFamily.setPreviousBeneficiary(true);
                        String existingYears = existingFamily.getPreviousBeneficiaryYears();
                        if (existingYears == null || existingYears.isEmpty()) {
                            existingFamily.setPreviousBeneficiaryYears(year);
                        } else if (!existingYears.contains(year)) {
                            existingFamily.setPreviousBeneficiaryYears(existingYears + ", " + year);
                        }
                        existingFamily.setPreviousBeneficiaryNotes("Importado do arquivo " + file.getOriginalFilename());
                        familyRepository.save(existingFamily);
                        updated++;
                    } else {
                        // Create new family marked as previous beneficiary
                        Family family = new Family();
                        family.setResponsibleName(responsibleName);
                        family.setCpf(normalizedCpf);
                        family.setSpouseName(spouseName.isEmpty() ? null : spouseName);
                        family.setHouseholdSize(Integer.parseInt(householdSizeStr));
                        family.setTotalIncome(new BigDecimal(totalIncomeStr));
                        family.setOrigin(FamilyOrigin.valueOf(origin.toUpperCase()));
                        family.setRequestReason(RequestReason.valueOf(requestReason.toUpperCase()));
                        family.setReferrerName(referrerName.isEmpty() ? null : referrerName);
                        family.setObservations(observations.isEmpty() ? null : observations);
                        family.setPreviousBeneficiary(true);
                        family.setPreviousBeneficiaryYears(year);
                        family.setPreviousBeneficiaryNotes("Importado do arquivo " + file.getOriginalFilename());
                        family.setStatus(FamilyStatus.ATENDIDA); // Mark as already attended
                        family.setCreatedAt(LocalDateTime.now());
                        family.setUpdatedAt(LocalDateTime.now());
                        familyRepository.save(family);
                        imported++;
                    }
                } catch (Exception e) {
                    errors++;
                    errorMessages.add("Linha " + lineNumber + ": " + e.getMessage());
                    log.error("Erro ao importar linha {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        return new ImportResult(imported, updated, errors, errorMessages);
    }

    public record ImportResult(int imported, int updated, int errors, List<String> errorMessages) {}
}
