package com.mercadosolidario.repository;

import com.mercadosolidario.model.Sponsor;
import com.mercadosolidario.model.enums.SponsorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long> {

    List<Sponsor> findByNameContainingIgnoreCase(String name);

    List<Sponsor> findByType(SponsorType type);

    List<Sponsor> findByActiveTrue();

    List<Sponsor> findByActiveTrueOrderByCreatedAtDesc();

    List<Sponsor> findByEmailContainingIgnoreCase(String email);
}
