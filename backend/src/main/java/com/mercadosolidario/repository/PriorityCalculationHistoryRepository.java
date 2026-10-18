package com.mercadosolidario.repository;

import com.mercadosolidario.model.PriorityCalculationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriorityCalculationHistoryRepository extends JpaRepository<PriorityCalculationHistory, Long> {

    List<PriorityCalculationHistory> findTop100ByOrderByCalculatedAtDescIdDesc();

    List<PriorityCalculationHistory> findByConfigurationIdOrderByCalculatedAtDescIdDesc(Long configurationId);

    Optional<PriorityCalculationHistory> findFirstByFamilyIdAndConfigurationIdOrderByCalculatedAtDescIdDesc(
        Long familyId, Long configurationId);
}
