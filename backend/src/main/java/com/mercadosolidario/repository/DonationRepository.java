package com.mercadosolidario.repository;

import com.mercadosolidario.model.Donation;
import com.mercadosolidario.model.enums.DonationStatus;
import com.mercadosolidario.model.enums.DonationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    Optional<Donation> findByExternalReference(String externalReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Donation d WHERE d.id = :id")
    Optional<Donation> findByIdForUpdate(@Param("id") Long id);

    List<Donation> findAllByOrderByCreatedAtDesc();

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime toExclusive);

    @Query("SELECT d.status, COUNT(d) FROM Donation d WHERE d.createdAt >= :from AND d.createdAt < :toExclusive GROUP BY d.status")
    List<Object[]> countGroupedByStatusInPeriod(
        @Param("from") LocalDateTime from, @Param("toExclusive") LocalDateTime toExclusive);

    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        DonationStatus status, LocalDateTime from, LocalDateTime toExclusive);

    long countByTypeAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        DonationType type, DonationStatus status, LocalDateTime from, LocalDateTime toExclusive);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Donation d WHERE d.type = :type AND d.status IN :statuses AND d.createdAt >= :from AND d.createdAt < :toExclusive")
    BigDecimal sumConfirmedFinancialAmount(
        @Param("type") DonationType type,
        @Param("statuses") List<DonationStatus> statuses,
        @Param("from") LocalDateTime from,
        @Param("toExclusive") LocalDateTime toExclusive
    );

    List<Donation> findByStatus(DonationStatus status);

    List<Donation> findBySponsorId(Long sponsorId);

    List<Donation> findByIsAnonymousTrue();

    List<Donation> findByDonorNameContainingIgnoreCase(String donorName);

    @Query("SELECT d FROM Donation d WHERE d.createdAt BETWEEN :startDate AND :endDate ORDER BY d.createdAt DESC")
    List<Donation> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT d FROM Donation d WHERE d.status = :status AND d.createdAt BETWEEN :startDate AND :endDate")
    List<Donation> findByStatusAndDateRange(
            @Param("status") DonationStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") DonationStatus status);

    @Query("SELECT COUNT(d) FROM Donation d WHERE d.status = :status")
    Long countByStatus(@Param("status") DonationStatus status);
}
