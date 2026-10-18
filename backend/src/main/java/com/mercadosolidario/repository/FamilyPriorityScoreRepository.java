package com.mercadosolidario.repository;

import com.mercadosolidario.model.FamilyPriorityScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyPriorityScoreRepository extends JpaRepository<FamilyPriorityScore, Long> {

    Optional<FamilyPriorityScore> findByFamilyId(Long familyId);

    @Query("SELECT fps FROM FamilyPriorityScore fps WHERE fps.priorityPosition IS NOT NULL ORDER BY fps.priorityPosition ASC")
    List<FamilyPriorityScore> findAllOrderByPriorityPosition();

    @Query("SELECT fps FROM FamilyPriorityScore fps ORDER BY fps.score DESC")
    List<FamilyPriorityScore> findAllOrderByScoreDesc();

    @Query("SELECT fps FROM FamilyPriorityScore fps WHERE fps.calculationVersion = :version")
    List<FamilyPriorityScore> findByCalculationVersion(String version);
}
