package com.mercadosolidario.repository;

import com.mercadosolidario.model.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    List<CashMovement> findAllByOrderByCreatedAtDescIdDesc();

    @Query("SELECT COALESCE(SUM(CASE WHEN m.type = 'ENTRADA' THEN m.amount ELSE -m.amount END), 0) FROM CashMovement m")
    BigDecimal balance();
}
