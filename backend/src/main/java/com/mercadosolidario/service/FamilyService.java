package com.mercadosolidario.service;

import com.mercadosolidario.dto.family.FamilyCreateRequest;
import com.mercadosolidario.dto.family.FamilyMemberRequest;
import com.mercadosolidario.dto.family.FamilyResponse;
import com.mercadosolidario.dto.family.FamilySearchRequest;
import com.mercadosolidario.dto.family.FamilyUpdateRequest;
import com.mercadosolidario.exception.ConflictException;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.FamilyMember;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.FamilyMemberRepository;
import com.mercadosolidario.util.CpfValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final AuditService auditService;

    @Transactional
    public FamilyResponse create(FamilyCreateRequest request) {
        // Validate CPF
        String cpf = CpfValidator.unformat(request.getCpf());
        if (!CpfValidator.isValid(cpf)) {
            throw new IllegalArgumentException("CPF inválido");
        }

        // Check CPF uniqueness
        if (familyRepository.existsByCpf(cpf)) {
            throw new ConflictException("CPF já cadastrado");
        }

        // Create family
        Family family = new Family();
        family.setResponsibleName(request.getResponsibleName());
        family.setCpf(cpf);
        family.setSpouseName(request.getSpouseName());
        family.setPhone(request.getPhone());
        family.setAddress(request.getAddress());
        family.setHouseholdSize(request.getHouseholdSize());
        family.setTotalIncome(request.getTotalIncome());
        family.setPaysRent(Boolean.TRUE.equals(request.getPaysRent()));
        family.setRentValue(Boolean.TRUE.equals(request.getPaysRent()) ? request.getRentValue() : null);
        family.setRequestReason(request.getRequestReason());
        family.setOrigin(request.getOrigin());
        family.setReferrerName(request.getReferrerName());
        family.setObservations(request.getObservations());
        family.setStatus(FamilyStatus.CADASTRADA);

        // Calculate dependents
        int membersCount = request.getMembers() != null ? request.getMembers().size() : 0;
        family.setDependents(membersCount);

        family = familyRepository.save(family);

        // Log audit
        auditService.logFamilyCreated(family.getId(), family.getResponsibleName());

        // Create members
        if (request.getMembers() != null && !request.getMembers().isEmpty()) {
            for (FamilyMemberRequest memberRequest : request.getMembers()) {
                FamilyMember member = new FamilyMember();
                member.setFamily(family);
                member.setName(memberRequest.getName());
                member.setCpf(memberRequest.getCpf() != null ? 
                    CpfValidator.unformat(memberRequest.getCpf()) : null);
                member.setBirthDate(memberRequest.getBirthDate());
                member.setRelationship(memberRequest.getRelationship());
                member.setObservations(memberRequest.getObservations());
                familyMemberRepository.save(member);
            }
        }

        return mapToResponse(family);
    }

    @Transactional(readOnly = true)
    public Page<FamilyResponse> search(FamilySearchRequest request, Pageable pageable) {
        // Build specification based on filters
        // For simplicity, using repository methods directly
        // This could be improved with Specification in the future
        
        Page<Family> families;
        
        if (request.getName() != null && !request.getName().isEmpty()) {
            families = familyRepository.findByResponsibleNameContainingIgnoreCase(
                request.getName(), pageable);
        } else if (request.getCpf() != null && !request.getCpf().isEmpty()) {
            String cpf = CpfValidator.unformat(request.getCpf());
            Family family = familyRepository.findByCpf(cpf)
                .orElseThrow(() -> new IllegalArgumentException("Família não encontrada"));
            List<Family> familyList = List.of(family);
            families = new PageImpl<>(familyList, pageable, 1);
        } else if (request.getStatus() != null) {
            families = familyRepository.findByStatus(request.getStatus(), pageable);
        } else {
            families = familyRepository.findAll(pageable);
        }

        return families.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public FamilyResponse findById(Long id) {
        Family family = familyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Família não encontrada"));
        return mapToResponse(family);
    }

    @Transactional
    public FamilyResponse update(Long id, FamilyUpdateRequest request) {
        Family family = familyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Família não encontrada"));

        family.setResponsibleName(request.getResponsibleName());
        family.setSpouseName(request.getSpouseName());
        family.setPhone(request.getPhone());
        family.setAddress(request.getAddress());
        family.setHouseholdSize(request.getHouseholdSize());
        family.setTotalIncome(request.getTotalIncome());
        family.setPaysRent(Boolean.TRUE.equals(request.getPaysRent()));
        family.setRentValue(Boolean.TRUE.equals(request.getPaysRent()) ? request.getRentValue() : null);
        family.setRequestReason(request.getRequestReason());
        family.setOrigin(request.getOrigin());
        family.setReferrerName(request.getReferrerName());
        family.setObservations(request.getObservations());

        // Update members
        if (request.getMembers() != null) {
            // Remove existing members
            familyMemberRepository.deleteByFamilyId(id);
            
            // Add new members
            for (FamilyMemberRequest memberRequest : request.getMembers()) {
                FamilyMember member = new FamilyMember();
                member.setFamily(family);
                member.setName(memberRequest.getName());
                member.setCpf(memberRequest.getCpf() != null ? 
                    CpfValidator.unformat(memberRequest.getCpf()) : null);
                member.setBirthDate(memberRequest.getBirthDate());
                member.setRelationship(memberRequest.getRelationship());
                member.setObservations(memberRequest.getObservations());
                familyMemberRepository.save(member);
            }
            
            family.setDependents(request.getMembers().size());
        }

        family = familyRepository.save(family);

        // Log audit
        auditService.logFamilyUpdated(family.getId(), family.getResponsibleName());

        return mapToResponse(family);
    }

    @Transactional
    public FamilyResponse updateStatus(Long id, FamilyStatus newStatus) {
        Family family = familyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Família não encontrada"));

        FamilyStatus currentStatus = family.getStatus();

        // Log audit before change
        auditService.logFamilyStatusChanged(id, currentStatus.name(), newStatus.name());

        // Validate status transition
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new IllegalArgumentException(
                "Transição de status inválida: " + currentStatus + " -> " + newStatus);
        }

        family.setStatus(newStatus);
        family = familyRepository.save(family);
        return mapToResponse(family);
    }

    private boolean isValidStatusTransition(FamilyStatus from, FamilyStatus to) {
        // Allow same status
        if (from == to) {
            return true;
        }

        // Define valid transitions
        switch (from) {
            case CADASTRADA:
                return to == FamilyStatus.EM_ANALISE || to == FamilyStatus.CANCELADA;
            case EM_ANALISE:
                return to == FamilyStatus.APROVADA || to == FamilyStatus.CANCELADA;
            case APROVADA:
                return to == FamilyStatus.CONTEMPLADA || to == FamilyStatus.INATIVA;
            case CONTEMPLADA:
                return to == FamilyStatus.ATENDIDA;
            case ATENDIDA:
                return false; // Final state
            case INATIVA:
                return to == FamilyStatus.CADASTRADA; // Reactivate
            case CANCELADA:
                return false; // Final state
            default:
                return false;
        }
    }

    private FamilyResponse mapToResponse(Family family) {
        return FamilyResponse.builder()
            .id(family.getId())
            .responsibleName(family.getResponsibleName())
            .cpf(CpfValidator.format(family.getCpf()))
            .spouseName(family.getSpouseName())
            .phone(family.getPhone())
            .address(family.getAddress())
            .dependents(family.getDependents())
            .householdSize(family.getHouseholdSize())
            .totalIncome(family.getTotalIncome())
            .paysRent(family.getPaysRent())
            .rentValue(family.getRentValue())
            .requestReason(family.getRequestReason())
            .origin(family.getOrigin())
            .referrerName(family.getReferrerName())
            .observations(family.getObservations())
            .previousBeneficiary(family.getPreviousBeneficiary())
            .previousBeneficiaryYears(family.getPreviousBeneficiaryYears())
            .previousBeneficiaryNotes(family.getPreviousBeneficiaryNotes())
            .status(family.getStatus())
            .createdAt(family.getCreatedAt())
            .updatedAt(family.getUpdatedAt())
            .members(family.getMembers().stream()
                .map(member -> FamilyResponse.FamilyMemberResponse.builder()
                    .id(member.getId())
                    .name(member.getName())
                    .cpf(member.getCpf() != null ? CpfValidator.format(member.getCpf()) : null)
                    .birthDate(member.getBirthDate())
                    .relationship(member.getRelationship())
                    .observations(member.getObservations())
                    .createdAt(member.getCreatedAt())
                    .updatedAt(member.getUpdatedAt())
                    .build())
                .collect(Collectors.toList()))
            .build();
    }
}
