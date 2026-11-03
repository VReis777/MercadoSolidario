package com.mercadosolidario.repository;

import com.mercadosolidario.model.RunRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RunRegistrationRepository extends JpaRepository<RunRegistration, Long> {

    @Query("SELECT COUNT(r) FROM RunRegistration r WHERE r.modality = :modality AND r.status <> 'CANCELADA'")
    long countActiveByModality(String modality);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RunRegistration r WHERE r.id = :id")
    Optional<RunRegistration> findByIdForUpdate(Long id);

    Optional<RunRegistration> findByPaymentRef(String paymentRef);

    List<RunRegistration> findAllByOrderByCreatedAtDesc();
}
