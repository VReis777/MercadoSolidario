package com.mercadosolidario.repository;

import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {

    Optional<Family> findByCpf(String cpf);

    long countByStatus(FamilyStatus status);

    @Query("SELECT f.status, COUNT(f) FROM Family f GROUP BY f.status")
    List<Object[]> countGroupedByStatus();

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime toExclusive);

    boolean existsByCpf(String cpf);

    List<Family> findByResponsibleNameContainingIgnoreCase(String name);

    Page<Family> findByResponsibleNameContainingIgnoreCase(String name, Pageable pageable);

    List<Family> findByStatus(FamilyStatus status);

    Page<Family> findByStatus(FamilyStatus status, Pageable pageable);

    List<Family> findByOrigin(FamilyOrigin origin);

    List<Family> findByStatusAndOrigin(FamilyStatus status, FamilyOrigin origin);

    @Query("SELECT f FROM Family f WHERE f.responsibleName LIKE %:name% OR f.cpf LIKE %:cpf%")
    List<Family> searchByNameOrCpf(@Param("name") String name, @Param("cpf") String cpf);

    @Query("SELECT f FROM Family f WHERE f.status = :status ORDER BY f.id DESC")
    List<Family> findByStatusOrderByIdDesc(@Param("status") FamilyStatus status);

    @Query("SELECT f FROM Family f WHERE f.householdSize >= :minSize")
    List<Family> findByHouseholdSizeGreaterThanEqual(@Param("minSize") Integer minSize);

    @Query("SELECT f FROM Family f WHERE f.totalIncome <= :maxIncome")
    List<Family> findByTotalIncomeLessThanEqual(@Param("maxIncome") java.math.BigDecimal maxIncome);

    Page<Family> findByPreviousBeneficiary(Boolean previousBeneficiary, Pageable pageable);

    List<Family> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime toExclusive);

    @Query("SELECT f.responsibleName, f.cpf FROM Family f")
    List<Object[]> findAllNamesAndCpfs();
}
