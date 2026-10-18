package com.mercadosolidario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.dto.priority.PriorityCalculationResponse;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.PriorityConfiguration;
import com.mercadosolidario.model.PriorityRule;
import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.model.enums.RequestReason;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.PriorityCalculationHistoryRepository;
import com.mercadosolidario.repository.PriorityConfigurationRepository;
import com.mercadosolidario.repository.UserRepository;
import com.mercadosolidario.service.PriorityCalculationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriorityCalculationServiceTest {

    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private PriorityConfigurationRepository configurationRepository;
    @Mock
    private PriorityCalculationHistoryRepository historyRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;

    private PriorityCalculationService service;

    @org.junit.jupiter.api.BeforeEach
    void createService() {
        service = new PriorityCalculationService(
            familyRepository,
            configurationRepository,
            historyRepository,
            auditLogRepository,
            userRepository,
            new ObjectMapper()
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noActiveConfigurationReturnsExplicitUnconfiguredState() {
        Family family = family();
        when(familyRepository.findById(5L)).thenReturn(Optional.of(family));
        when(configurationRepository.findFirstByActiveTrueOrderByVersionNumberDesc()).thenReturn(Optional.empty());

        PriorityCalculationResponse result = service.calculate(5L);

        assertThat(result.status()).isEqualTo("PRIORIDADE_NAO_CONFIGURADA");
        assertThat(result.score()).isNull();
        assertThat(result.factors()).isEmpty();
        assertThat(family.getStatus()).isEqualTo(FamilyStatus.CADASTRADA);
        verifyNoInteractions(historyRepository, auditLogRepository);
    }

    @Test
    void calculationUsesOnlyEnabledFactorsAndReturnsExplanationAndVersion() {
        authenticate();
        Family family = family();
        PriorityConfiguration configuration = configuration();
        configuration.setRules(List.of(
            rule(configuration, "HOUSEHOLD_SIZE", "GTE", "5", "10.00", true),
            rule(configuration, "TOTAL_INCOME", "LTE", "1000", "99.00", false)
        ));
        when(familyRepository.findById(5L)).thenReturn(Optional.of(family));
        when(configurationRepository.findFirstByActiveTrueOrderByVersionNumberDesc())
            .thenReturn(Optional.of(configuration));
        when(userRepository.findByUsername("admin-demo")).thenReturn(Optional.empty());
        when(historyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PriorityCalculationResponse result = service.calculate(5L);

        assertThat(result.status()).isEqualTo("CALCULADA");
        assertThat(result.score()).isEqualByComparingTo("10.00");
        assertThat(result.calculationVersion()).isEqualTo("CONFIG-DEMO-v2");
        assertThat(result.factors()).hasSize(1);
        assertThat(result.factors().getFirst().inputValue()).isEqualTo("6");
        assertThat(result.factors().getFirst().contribution()).isEqualByComparingTo("10.00");
        assertThat(result.factors().getFirst().explanation()).isEqualTo("Explicação cadastrada para demonstração técnica.");
        assertThat(family.getStatus()).isEqualTo(FamilyStatus.CADASTRADA);
        verify(historyRepository).save(any());
        verify(auditLogRepository).save(any());
    }

    @Test
    void missingFamilyReturnsNotFound() {
        when(familyRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(404L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Família não encontrada");
        verifyNoInteractions(configurationRepository, historyRepository);
    }

    @Test
    void rankingAssignsSamePositionToEqualScoresAndStoresHistory() {
        authenticate();
        Family first = family();
        first.setId(5L);
        Family second = family();
        second.setId(6L);
        PriorityConfiguration configuration = configuration();
        configuration.setRules(List.of(rule(configuration, "HOUSEHOLD_SIZE", "GTE", "5", "10.00", true)));
        when(configurationRepository.findFirstByActiveTrueOrderByVersionNumberDesc())
            .thenReturn(Optional.of(configuration));
        when(familyRepository.findAll()).thenReturn(List.of(first, second));
        when(userRepository.findByUsername("admin-demo")).thenReturn(Optional.empty());
        when(historyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var ranking = service.calculateRanking();

        assertThat(ranking.status()).isEqualTo("CALCULADA");
        assertThat(ranking.families()).hasSize(2);
        assertThat(ranking.families()).allSatisfy(result -> assertThat(result.position()).isEqualTo(1));
        verify(historyRepository, times(2)).save(any());
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin-demo", "N/A", List.of()));
    }

    private Family family() {
        Family family = new Family();
        family.setId(5L);
        family.setTotalIncome(new BigDecimal("900.00"));
        family.setHouseholdSize(6);
        family.setDependents(4);
        family.setRequestReason(RequestReason.BAIXA_RENDA);
        family.setOrigin(FamilyOrigin.CADASTRO_PROPRIO);
        family.setStatus(FamilyStatus.CADASTRADA);
        return family;
    }

    private PriorityConfiguration configuration() {
        PriorityConfiguration configuration = new PriorityConfiguration();
        configuration.setId(2L);
        configuration.setCode("CONFIG-DEMO");
        configuration.setVersionNumber(2);
        return configuration;
    }

    private PriorityRule rule(
            PriorityConfiguration configuration, String code, String operator,
            String reference, String weight, boolean enabled) {
        PriorityRule rule = new PriorityRule();
        rule.setConfiguration(configuration);
        rule.setFactorCode(code);
        rule.setLabel("Fator técnico de demonstração");
        rule.setOperator(operator);
        rule.setReferenceValue(reference);
        rule.setWeight(new BigDecimal(weight));
        rule.setEnabled(enabled);
        rule.setExplanation("Explicação cadastrada para demonstração técnica.");
        return rule;
    }
}
