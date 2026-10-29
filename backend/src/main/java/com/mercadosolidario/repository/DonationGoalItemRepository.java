package com.mercadosolidario.repository;

import com.mercadosolidario.model.DonationGoalItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationGoalItemRepository extends JpaRepository<DonationGoalItem, Long> {
    List<DonationGoalItem> findByActiveTrueOrderBySortOrderAsc();
}
