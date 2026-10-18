package com.mercadosolidario.repository;

import com.mercadosolidario.model.ImportError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportErrorRepository extends JpaRepository<ImportError, Long> {

    List<ImportError> findByImportBatchId(Long importBatchId);

    List<ImportError> findByImportBatchIdOrderByRowNumberAsc(Long importBatchId);
}
