package com.mercadosolidario.repository;

import com.mercadosolidario.model.PriorityConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriorityConfigurationRepository extends JpaRepository<PriorityConfiguration, Long> {

    List<PriorityConfiguration> findAllByOrderByCodeAscVersionNumberDesc();

    Optional<PriorityConfiguration> findFirstByActiveTrueOrderByVersionNumberDesc();

    Optional<PriorityConfiguration> findFirstByCodeOrderByVersionNumberDesc(String code);

    boolean existsByCode(String code);
}
