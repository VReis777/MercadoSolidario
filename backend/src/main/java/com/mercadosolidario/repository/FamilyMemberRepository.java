package com.mercadosolidario.repository;

import com.mercadosolidario.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    List<FamilyMember> findByFamilyId(Long familyId);

    List<FamilyMember> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    @Transactional
    void deleteByFamilyId(Long familyId);
}
