package com.mercadosolidario.repository;

import com.mercadosolidario.model.Distribution;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import com.mercadosolidario.model.enums.DistributionStatus;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DistributionRepository extends JpaRepository<Distribution, Long> {

    java.util.Optional<Distribution> findByIdempotencyKey(String idempotencyKey);

    List<Distribution> findAllByOrderByCreatedAtDesc();

    long countByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
        DistributionStatus status, LocalDateTime from, LocalDateTime toExclusive);

    @Query("SELECT COUNT(DISTINCT d.family.id) FROM Distribution d WHERE d.status = :status AND d.completedAt >= :from AND d.completedAt < :toExclusive")
    long countDistinctFamiliesServed(
        @Param("status") DistributionStatus status,
        @Param("from") LocalDateTime from,
        @Param("toExclusive") LocalDateTime toExclusive
    );

    @EntityGraph(attributePaths = "family")
    @Query("SELECT d FROM Distribution d ORDER BY d.completedAt DESC, d.id DESC")
    List<Distribution> findRecent(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Distribution d WHERE d.id = :id")
    java.util.Optional<Distribution> findByIdForUpdate(@Param("id") Long id);

    List<Distribution> findByFamilyId(Long familyId);

    List<Distribution> findByProductId(Long productId);

    List<Distribution> findByUserId(Long userId);

    List<Distribution> findByFamilyIdOrderByDistributionDateDesc(Long familyId);

    @Query("SELECT d FROM Distribution d WHERE d.distributionDate BETWEEN :startDate AND :endDate ORDER BY d.distributionDate DESC")
    List<Distribution> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT d FROM Distribution d WHERE d.family.id = :familyId AND d.distributionDate BETWEEN :startDate AND :endDate")
    List<Distribution> findByFamilyIdAndDateRange(
            @Param("familyId") Long familyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(d) FROM Distribution d WHERE d.family.id = :familyId")
    Long countByFamilyId(@Param("familyId") Long familyId);
}
