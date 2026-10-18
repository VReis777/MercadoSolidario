package com.mercadosolidario.repository;

import com.mercadosolidario.model.PublicServiceRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicServiceRequestHistoryRepository extends JpaRepository<PublicServiceRequestHistory, Long> {
    List<PublicServiceRequestHistory> findByServiceRequestIdOrderByCreatedAtAscIdAsc(Long serviceRequestId);
}
