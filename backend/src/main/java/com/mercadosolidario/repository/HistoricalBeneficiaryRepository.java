package com.mercadosolidario.repository;

import com.mercadosolidario.model.HistoricalBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface HistoricalBeneficiaryRepository
        extends JpaRepository<HistoricalBeneficiary, Long>, JpaSpecificationExecutor<HistoricalBeneficiary> {

    List<HistoricalBeneficiary> findByCampaignYear(Integer campaignYear);

    List<HistoricalBeneficiary> findByCpfIn(Collection<String> cpfs);

    List<HistoricalBeneficiary> findByNormalizedNameIn(Collection<String> normalizedNames);

    @Query("SELECT h FROM HistoricalBeneficiary h WHERE (:cpf IS NOT NULL AND h.cpf = :cpf) OR h.normalizedName = :normalizedName")
    List<HistoricalBeneficiary> findByCpfOrNormalizedName(String cpf, String normalizedName);

    @Query("SELECT h.campaignYear, COUNT(h) FROM HistoricalBeneficiary h GROUP BY h.campaignYear ORDER BY h.campaignYear")
    List<Object[]> countByYear();

    @Query("SELECT h.campaignYear, COUNT(h) FROM HistoricalBeneficiary h WHERE h.cpf IS NULL GROUP BY h.campaignYear")
    List<Object[]> countWithoutCpfByYear();

    @Query("SELECT DISTINCT h.origin FROM HistoricalBeneficiary h WHERE h.origin IS NOT NULL ORDER BY h.origin")
    List<String> findDistinctOrigins();

    @Query("""
        SELECT COUNT(DISTINCT a.normalizedName) FROM HistoricalBeneficiary a, HistoricalBeneficiary b
        WHERE a.campaignYear <> b.campaignYear
          AND ((a.cpf IS NOT NULL AND a.cpf = b.cpf) OR a.normalizedName = b.normalizedName)
        """)
    long countPeopleInMoreThanOneYear();
}
