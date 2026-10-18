package com.mercadosolidario.repository;

import com.mercadosolidario.model.StockMovement;
import com.mercadosolidario.model.enums.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductId(Long productId);

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<StockMovement> findTop200ByOrderByCreatedAtDescIdDesc();

    List<StockMovement> findByType(MovementType type);

    List<StockMovement> findByProductIdAndType(Long productId, MovementType type);

    List<StockMovement> findByFamilyId(Long familyId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.product.id = :productId AND sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findByProductIdAndDateRange(
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT sm FROM StockMovement sm WHERE sm.createdAt BETWEEN :startDate AND :endDate ORDER BY sm.createdAt DESC")
    List<StockMovement> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT sm FROM StockMovement sm WHERE sm.type = :type AND sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findByTypeAndDateRange(
            @Param("type") MovementType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT sm.type, COUNT(sm) FROM StockMovement sm WHERE sm.createdAt >= :from AND sm.createdAt < :toExclusive GROUP BY sm.type")
    List<Object[]> countByTypeInPeriod(@Param("from") LocalDateTime from, @Param("toExclusive") LocalDateTime toExclusive);

    @EntityGraph(attributePaths = "product")
    @Query("SELECT sm FROM StockMovement sm ORDER BY sm.createdAt DESC, sm.id DESC")
    List<StockMovement> findRecent(Pageable pageable);

    @Query("SELECT COUNT(sm) FROM StockMovement sm WHERE sm.type = :type AND sm.reason = :reason AND sm.createdAt >= :from AND sm.createdAt < :toExclusive")
    long countDonationEntries(
        @Param("type") MovementType type,
        @Param("reason") String reason,
        @Param("from") LocalDateTime from,
        @Param("toExclusive") LocalDateTime toExclusive
    );
}
