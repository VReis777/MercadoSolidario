package com.mercadosolidario.repository;

import com.mercadosolidario.model.DistributionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.mercadosolidario.model.enums.DistributionStatus;

@Repository
public interface DistributionItemRepository extends JpaRepository<DistributionItem, Long> {

    List<DistributionItem> findByDistributionIdOrderByIdAsc(Long distributionId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM DistributionItem i WHERE i.distribution.status = :status AND i.distribution.completedAt >= :from AND i.distribution.completedAt < :toExclusive")
    long sumDistributedQuantity(
        @Param("status") DistributionStatus status,
        @Param("from") LocalDateTime from,
        @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query("SELECT i.product.id, i.product.name, SUM(i.quantity), i.unit FROM DistributionItem i WHERE i.distribution.status = :status AND i.distribution.completedAt >= :from AND i.distribution.completedAt < :toExclusive GROUP BY i.product.id, i.product.name, i.unit ORDER BY SUM(i.quantity) DESC")
    List<Object[]> topProducts(
        @Param("status") DistributionStatus status,
        @Param("from") LocalDateTime from,
        @Param("toExclusive") LocalDateTime toExclusive,
        org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT d.id, COUNT(i) FROM Distribution d LEFT JOIN d.items i WHERE d.id IN :ids GROUP BY d.id")
    List<Object[]> countByDistributionIds(@Param("ids") List<Long> ids);
}
