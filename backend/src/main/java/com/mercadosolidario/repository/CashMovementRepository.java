package com.mercadosolidario.repository;

import com.mercadosolidario.model.CashMovement;
import com.mercadosolidario.model.enums.CashMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    List<CashMovement> findAllByOrderByCreatedAtDescIdDesc();

    @Query("SELECT COALESCE(SUM(CASE WHEN m.type = 'ENTRADA' THEN m.amount ELSE -m.amount END), 0) FROM CashMovement m")
    BigDecimal balance();

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM CashMovement m WHERE m.type = :type AND m.createdAt >= :from AND m.createdAt < :toExclusive")
    BigDecimal sumByTypeInPeriod(
        @org.springframework.data.repository.query.Param("type") CashMovementType type,
        @org.springframework.data.repository.query.Param("from") LocalDateTime from,
        @org.springframework.data.repository.query.Param("toExclusive") LocalDateTime toExclusive);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime toExclusive);
}
