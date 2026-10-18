package com.mercadosolidario.repository;

import com.mercadosolidario.model.WelcomeAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WelcomeAvailabilityRepository extends JpaRepository<WelcomeAvailability, Long> {

    List<WelcomeAvailability> findByActiveTrueOrderByAvailableDateAsc();

    long countByActiveTrue();

    boolean existsByAvailableDateAndActiveTrue(LocalDate date);

    boolean existsByAvailableDate(LocalDate date);
}
