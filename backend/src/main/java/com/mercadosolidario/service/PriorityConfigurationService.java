package com.mercadosolidario.service;

import com.mercadosolidario.dto.priority.PriorityConfigurationRequest;
import com.mercadosolidario.dto.priority.PriorityConfigurationResponse;
import com.mercadosolidario.dto.priority.PriorityFactorConfigurationResponse;
import com.mercadosolidario.dto.priority.PriorityFactorRequest;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.AuditLog;
import com.mercadosolidario.model.PriorityConfiguration;
import com.mercadosolidario.model.PriorityRule;
import com.mercadosolidario.model.User;
import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.RequestReason;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.PriorityConfigurationRepository;
import com.mercadosolidario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriorityConfigurationService {

    private static final Set<String> NUMERIC_FACTORS = Set.of("TOTAL_INCOME", "PER_CAPITA_INCOME", "HOUSEHOLD_SIZE", "DEPENDENTS", "EFFECTIVE_PER_CAPITA_INCOME", "NO_RENT_INCOME", "NO_RENT_INCOME_SMALL", "SINGLE_PARENT_DEPENDENTS");
    private static final Set<String> ENUM_FACTORS = Set.of("REQUEST_REASON", "ORIGIN");
    private static final Set<String> NUMERIC_OPERATORS = Set.of("EQ", "NE", "GT", "GTE", "LT", "LTE");
    private static final Set<String> ENUM_OPERATORS = Set.of("EQ", "NE");

    private final PriorityConfigurationRepository configurationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PriorityConfigurationResponse> list() {
        return configurationRepository.findAllByOrderByCodeAscVersionNumberDesc()
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PriorityConfigurationResponse create(PriorityConfigurationRequest request) {
        String code = request.getCode().trim();
        if (configurationRepository.existsByCode(code)) {
            throw new ConflictException("O código já possui uma configuração versionada. Crie uma nova versão.");
        }
        String actor = currentActor();
        PriorityConfiguration configuration = newConfiguration(request, code, 1, actor);
        PriorityConfiguration saved = configurationRepository.save(configuration);
        audit("PRIORITY_CONFIGURATION_CREATED", saved);
        return toResponse(saved);
    }

    @Transactional
    public PriorityConfigurationResponse createVersion(Long id, PriorityConfigurationRequest request) {
        PriorityConfiguration previous = requireConfiguration(id);
        if (!previous.getCode().equals(request.getCode().trim())) {
            throw new ConflictException("O código da configuração não pode ser alterado entre versões.");
        }
        PriorityConfiguration latestVersion = configurationRepository
            .findFirstByCodeOrderByVersionNumberDesc(previous.getCode())
            .orElseThrow(() -> new ResourceNotFoundException("Configuração de prioridade não encontrada"));
        String actor = currentActor();
        if (previous.isActive()) {
            previous.setActive(false);
            previous.setUpdatedBy(actor);
            configurationRepository.save(previous);
            audit("PRIORITY_CONFIGURATION_DEACTIVATED", previous);
        }
        PriorityConfiguration version = newConfiguration(
            request, previous.getCode(), latestVersion.getVersionNumber() + 1, actor);
        PriorityConfiguration saved = configurationRepository.save(version);
        audit("PRIORITY_CONFIGURATION_VERSION_CREATED", saved);
        return toResponse(saved);
    }

    @Transactional
    public PriorityConfigurationResponse activate(Long id) {
        PriorityConfiguration target = requireConfiguration(id);
        if (target.getRules().stream().noneMatch(PriorityRule::isEnabled)) {
            throw new ConflictException("Ative pelo menos um fator antes de ativar a configuração.");
        }
        String actor = currentActor();
        configurationRepository.findAllByOrderByCodeAscVersionNumberDesc().stream()
            .filter(PriorityConfiguration::isActive)
            .filter(configuration -> !configuration.getId().equals(id))
            .forEach(configuration -> {
                configuration.setActive(false);
                configuration.setUpdatedBy(actor);
                configurationRepository.save(configuration);
                audit("PRIORITY_CONFIGURATION_DEACTIVATED", configuration);
            });
        target.setActive(true);
        target.setUpdatedBy(actor);
        PriorityConfiguration saved = configurationRepository.save(target);
        audit("PRIORITY_CONFIGURATION_ACTIVATED", saved);
        return toResponse(saved);
    }

    @Transactional
    public PriorityConfigurationResponse deactivate(Long id) {
        PriorityConfiguration configuration = requireConfiguration(id);
        configuration.setActive(false);
        configuration.setUpdatedBy(currentActor());
        PriorityConfiguration saved = configurationRepository.save(configuration);
        audit("PRIORITY_CONFIGURATION_DEACTIVATED", saved);
        return toResponse(saved);
    }

    private PriorityConfiguration newConfiguration(
            PriorityConfigurationRequest request, String code, int version, String actor) {
        validateRules(request.getFactors());
        PriorityConfiguration configuration = new PriorityConfiguration();
        configuration.setCode(code);
        configuration.setName(request.getName().trim());
        configuration.setVersionNumber(version);
        configuration.setExplanation(request.getExplanation().trim());
        configuration.setActive(false);
        configuration.setCreatedBy(actor);
        configuration.setUpdatedBy(actor);

        List<PriorityRule> rules = request.getFactors().stream().map(factor -> {
            PriorityRule rule = new PriorityRule();
            rule.setConfiguration(configuration);
            rule.setFactorCode(factor.getFactorCode().trim().toUpperCase(Locale.ROOT));
            rule.setLabel(factor.getLabel().trim());
            rule.setOperator(factor.getOperator().trim().toUpperCase(Locale.ROOT));
            rule.setReferenceValue(factor.getReferenceValue().trim());
            rule.setWeight(factor.getWeight());
            rule.setEnabled(factor.isEnabled());
            rule.setExplanation(factor.getExplanation().trim());
            return rule;
        }).collect(Collectors.toCollection(ArrayList::new));
        configuration.setRules(rules);
        return configuration;
    }

    private void validateRules(List<PriorityFactorRequest> factors) {
        for (PriorityFactorRequest factor : factors) {
            String code = factor.getFactorCode().trim().toUpperCase(Locale.ROOT);
            String operator = factor.getOperator().trim().toUpperCase(Locale.ROOT);
            if (!NUMERIC_FACTORS.contains(code) && !ENUM_FACTORS.contains(code)) {
                throw new IllegalArgumentException("Fator não suportado: " + code);
            }
            Set<String> allowedOperators = NUMERIC_FACTORS.contains(code) ? NUMERIC_OPERATORS : ENUM_OPERATORS;
            if (!allowedOperators.contains(operator)) {
                throw new IllegalArgumentException("Operador não permitido para o fator " + code);
            }
            if (NUMERIC_FACTORS.contains(code)) {
                try {
                    new BigDecimal(factor.getReferenceValue().trim());
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("O valor de referência do fator " + code + " deve ser numérico.");
                }
            } else if (!enumValues(code).contains(factor.getReferenceValue().trim().toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("O valor de referência não existe para o fator " + code);
            }
        }
    }

    private Set<String> enumValues(String factorCode) {
        if (factorCode.equals("REQUEST_REASON")) {
            return java.util.Arrays.stream(RequestReason.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet());
        }
        return java.util.Arrays.stream(FamilyOrigin.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet());
    }

    private PriorityConfiguration requireConfiguration(Long id) {
        return configurationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Configuração de prioridade não encontrada"));
    }

    private PriorityConfigurationResponse toResponse(PriorityConfiguration configuration) {
        List<PriorityFactorConfigurationResponse> factors = configuration.getRules().stream()
            .map(rule -> new PriorityFactorConfigurationResponse(
                rule.getFactorCode(),
                rule.getLabel(),
                rule.getOperator(),
                rule.getReferenceValue(),
                rule.getWeight(),
                rule.isEnabled(),
                rule.getExplanation()))
            .toList();
        return new PriorityConfigurationResponse(
            configuration.getId(),
            configuration.getCode(),
            configuration.getName(),
            configuration.getVersionNumber(),
            configuration.getExplanation(),
            configuration.isActive(),
            configuration.getCreatedBy(),
            configuration.getUpdatedBy(),
            configuration.getCreatedAt(),
            configuration.getUpdatedAt(),
            factors
        );
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Usuário autenticado obrigatório para gerenciar prioridades.");
        }
        return authentication.getName();
    }

    private void audit(String action, PriorityConfiguration configuration) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType("PriorityConfiguration");
        entry.setEntityId(configuration.getId());
        entry.setDetails("{\"code\":\"" + configuration.getCode()
            + "\",\"version\":" + configuration.getVersionNumber() + "}");
        userRepository.findByUsername(configuration.getUpdatedBy()).ifPresent(entry::setUser);
        auditLogRepository.save(entry);
    }
}
