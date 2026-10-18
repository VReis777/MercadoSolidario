package com.mercadosolidario.repository;

import com.mercadosolidario.model.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    List<ImportBatch> findByUserId(Long userId);

    List<ImportBatch> findByStatus(String status);

    List<ImportBatch> findByUserIdOrderByCreatedAtDesc(Long userId);
}
