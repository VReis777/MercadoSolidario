package com.mercadosolidario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.dto.priority.PriorityCalculationResponse;
import com.mercadosolidario.dto.priority.PriorityFactorResponse;
import com.mercadosolidario.dto.priority.PriorityRankingResponse;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.AuditLog;
import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.PriorityCalculationHistory;
import com.mercadosolidario.model.PriorityConfiguration;
import com.mercadosolidario.model.PriorityRule;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.PriorityCalculationHistoryRepository;
import com.mercadosolidario.repository.PriorityConfigurationRepository;
import com.mercadosolidario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PriorityCalculationService {

    private static final String UNCONFIGURED = "PRIORIDADE_NAO_CONFIGURADA";

    private final FamilyRepository familyRepository;
    private final PriorityConfigurationRepository configurationRepository;
    private final PriorityCalculationHistoryRepository historyRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PriorityCalculationResponse calculate(Long familyId) {
        Family family = familyRepository.findById(familyId)
            .orElseThrow(() -> new ResourceNotFoundException("Família não encontrada"));
        Optional<PriorityConfiguration> active = configurationRepository
            .findFirstByActiveTrueOrderByVersionNumberDesc();
        if (active.isEmpty()) {
            return unconfigured(familyId);
        }
        return calculate(family, active.get(), null, true);
    }

    @Transactional
    public PriorityCalculationResponse getFamilyPriority(Long familyId) {
        Family family = familyRepository.findById(familyId)
            .orElseThrow(() -> new ResourceNotFoundException("Família não encontrada"));
        Optional<PriorityConfiguration> active = configurationRepository
            .findFirstByActiveTrueOrderByVersionNumberDesc();
        if (active.isEmpty()) {
            return unconfigured(familyId);
        }
        Optional<PriorityCalculationHistory> history = historyRepository
            .findFirstByFamilyIdAndConfigurationIdOrderByCalculatedAtDescIdDesc(familyId, active.get().getId());
        return history.map(this::toResponse)
            .orElseGet(() -> pendingCalculation(familyId, active.get()));
    }

    @Transactional(readOnly = true)
    public PriorityRankingResponse getRanking() {
        Optional<PriorityConfiguration> active = configurationRepository
            .findFirstByActiveTrueOrderByVersionNumberDesc();
        if (active.isEmpty()) {
            return new PriorityRankingResponse(UNCONFIGURED, null, null, List.of());
        }
        List<PriorityCalculationHistory> histories = historyRepository
            .findByConfigurationIdOrderByCalculatedAtDescIdDesc(active.get().getId());
        List<Family> currentFamilies = familyRepository.findAll();
        Map<Long, PriorityCalculationHistory> latestByFamily = new LinkedHashMap<>();
        histories.forEach(history -> latestByFamily.putIfAbsent(history.getFamily().getId(), history));
        if (latestByFamily.isEmpty()) {
            return new PriorityRankingResponse(
                currentFamilies.isEmpty() ? "CALCULADA" : "CALCULO_PENDENTE",
                active.get().getCode() + "-v" + active.get().getVersionNumber(),
                null,
                List.of()
            );
        }
        LocalDateTime latestCalculation = histories.stream()
            .map(PriorityCalculationHistory::getCalculatedAt)
            .filter(value -> value != null)
            .max(Comparator.naturalOrder())
            .orElse(null);
        boolean stale = currentFamilies.size() != latestByFamily.size()
            || currentFamilies.stream()
                .map(Family::getUpdatedAt)
                .filter(value -> value != null)
                .anyMatch(updatedAt -> latestCalculation == null || updatedAt.isAfter(latestCalculation));
        if (stale) {
            return new PriorityRankingResponse(
                "RECALCULO_NECESSARIO",
                active.get().getCode() + "-v" + active.get().getVersionNumber(),
                latestCalculation,
                List.of()
            );
        }
        List<PriorityCalculationResponse> results = latestByFamily.values().stream()
            .map(this::toResponse)
            .sorted(Comparator
                .comparing(PriorityCalculationResponse::position, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PriorityCalculationResponse::familyId))
            .toList();
        return new PriorityRankingResponse(
            "CALCULADA",
            active.get().getCode() + "-v" + active.get().getVersionNumber(),
            latestCalculation,
            results
        );
    }

    @Transactional(readOnly = true)
    public List<PriorityCalculationResponse> getHistory() {
        return historyRepository.findTop100ByOrderByCalculatedAtDescIdDesc()
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PriorityRankingResponse calculateRanking() {
        Optional<PriorityConfiguration> active = configurationRepository
            .findFirstByActiveTrueOrderByVersionNumberDesc();
        if (active.isEmpty()) {
            return new PriorityRankingResponse(UNCONFIGURED, null, null, List.of());
        }
        PriorityConfiguration configuration = active.get();
        List<Family> families = familyRepository.findAll();
        Map<Long, Family> familyById = new LinkedHashMap<>();
        List<PriorityCalculationResponse> results = new ArrayList<>();
        for (Family family : families) {
            familyById.put(family.getId(), family);
            results.add(calculate(family, configuration, null, false));
        }
        results.sort(Comparator
            .comparing(PriorityCalculationResponse::score, Comparator.reverseOrder())
            .thenComparing(PriorityCalculationResponse::familyId));
        Integer position = null;
        BigDecimal previousScore = null;
        for (int index = 0; index < results.size(); index++) {
            PriorityCalculationResponse result = results.get(index);
            if (previousScore == null || result.score().compareTo(previousScore) != 0) {
                position = index + 1;
                previousScore = result.score();
            }
            results.set(index, withPosition(result, position));
        }
        results.forEach(result -> {
            Family family = familyById.get(result.familyId());
            saveHistory(family, configuration, result);
        });
        return new PriorityRankingResponse(
            "CALCULADA",
            configuration.getCode() + "-v" + configuration.getVersionNumber(),
            results.stream().map(PriorityCalculationResponse::calculatedAt)
                .max(Comparator.naturalOrder()).orElse(null),
            List.copyOf(results)
        );
    }

    private PriorityCalculationResponse calculate(
            Family family, PriorityConfiguration configuration, Integer position, boolean saveHistory) {
        List<PriorityFactorResponse> factors = configuration.getRules().stream()
            .filter(PriorityRule::isEnabled)
            .map(rule -> evaluate(family, rule))
            .toList();
        BigDecimal score = factors.stream()
            .map(PriorityFactorResponse::contribution)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        String version = configuration.getCode() + "-v" + configuration.getVersionNumber();
        LocalDateTime calculatedAt = LocalDateTime.now();
        PriorityCalculationResponse response = new PriorityCalculationResponse(
            family.getId(), score, position, calculatedAt, version, "CALCULADA", factors, false);
        if (saveHistory) {
            saveHistory(family, configuration, response);
        }
        return response;
    }

    private PriorityFactorResponse evaluate(Family family, PriorityRule rule) {
        Object input = inputValue(family, rule.getFactorCode());
        String inputValue = String.valueOf(input);
        String reference = rule.getReferenceValue();
        int comparison = input instanceof BigDecimal number
            ? number.compareTo(new BigDecimal(reference))
            : inputValue.compareToIgnoreCase(reference);
        boolean matched = switch (rule.getOperator().toUpperCase(Locale.ROOT)) {
            case "EQ" -> comparison == 0;
            case "NE" -> comparison != 0;
            case "GT" -> comparison > 0;
            case "GTE" -> comparison >= 0;
            case "LT" -> comparison < 0;
            case "LTE" -> comparison <= 0;
            default -> throw new IllegalStateException("Operador de prioridade persistido não reconhecido.");
        };
        return new PriorityFactorResponse(
            rule.getFactorCode(),
            rule.getLabel(),
            inputValue,
            matched ? rule.getWeight() : BigDecimal.ZERO,
            matched,
            rule.getExplanation()
        );
    }

    private Object inputValue(Family family, String factorCode) {
        return switch (factorCode) {
            case "TOTAL_INCOME" -> family.getTotalIncome();
            case "PER_CAPITA_INCOME" -> perCapitaIncome(family);
            case "HOUSEHOLD_SIZE" -> BigDecimal.valueOf(family.getHouseholdSize());
            case "DEPENDENTS" -> BigDecimal.valueOf(family.getDependents() == null ? 0 : family.getDependents());
            case "REQUEST_REASON" -> family.getRequestReason().name();
            case "ORIGIN" -> family.getOrigin().name();
            default -> throw new IllegalStateException("Fator de prioridade persistido não reconhecido.");
        };
    }

    private BigDecimal perCapitaIncome(Family family) {
        int people = family.getHouseholdSize() == null || family.getHouseholdSize() < 1
            ? 1 : family.getHouseholdSize();
        BigDecimal income = family.getTotalIncome() == null
            ? BigDecimal.ZERO : family.getTotalIncome();
        return income.divide(BigDecimal.valueOf(people), 2, java.math.RoundingMode.HALF_UP);
    }

    private PriorityCalculationResponse unconfigured(Long familyId) {
        return new PriorityCalculationResponse(
            familyId,
            null,
            null,
            null,
            null,
            UNCONFIGURED,
            List.of(),
            false
        );
    }

    private PriorityCalculationResponse pendingCalculation(Long familyId, PriorityConfiguration configuration) {
        return new PriorityCalculationResponse(
            familyId,
            null,
            null,
            null,
            configuration.getCode() + "-v" + configuration.getVersionNumber(),
            "CALCULO_PENDENTE",
            List.of(),
            false
        );
    }

    private PriorityCalculationResponse toResponse(PriorityCalculationHistory history) {
        List<PriorityFactorResponse> factors;
        try {
            factors = objectMapper.readValue(
                history.getFactorSnapshot(), new TypeReference<List<PriorityFactorResponse>>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("O histórico de prioridade está ilegível.", exception);
        }
        return new PriorityCalculationResponse(
            history.getFamily().getId(),
            history.getScore(),
            history.getPriorityPosition(),
            history.getCalculatedAt(),
            history.getCalculationVersion(),
            "CALCULADA",
            factors,
            false
        );
    }

    private PriorityCalculationResponse withPosition(PriorityCalculationResponse result, Integer position) {
        return new PriorityCalculationResponse(
            result.familyId(),
            result.score(),
            position,
            result.calculatedAt(),
            result.calculationVersion(),
            result.status(),
            result.factors(),
            result.demonstration()
        );
    }

    private void saveHistory(
            Family family, PriorityConfiguration configuration, PriorityCalculationResponse response) {
        String factorSnapshot;
        try {
            factorSnapshot = objectMapper.writeValueAsString(response.factors());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível preservar o histórico do cálculo de prioridade.", exception);
        }
        String actor = currentActor();
        PriorityCalculationHistory history = new PriorityCalculationHistory();
        history.setFamily(family);
        history.setConfiguration(configuration);
        history.setScore(response.score());
        history.setPriorityPosition(response.position());
        history.setCalculationVersion(response.calculationVersion());
        history.setFactorSnapshot(factorSnapshot);
        history.setCalculatedBy(actor);
        historyRepository.save(history);

        AuditLog audit = new AuditLog();
        audit.setAction("PRIORITY_CALCULATED");
        audit.setEntityType("Family");
        audit.setEntityId(family.getId());
        audit.setDetails("{\"configurationCode\":\"" + configuration.getCode()
            + "\",\"version\":" + configuration.getVersionNumber() + "}");
        userRepository.findByUsername(actor).ifPresent(audit::setUser);
        auditLogRepository.save(audit);
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Usuário autenticado obrigatório para calcular prioridade.");
        }
        return authentication.getName();
    }
}
