package com.mercadosolidario.controller;

import com.mercadosolidario.dto.family.FamilyMemberRequest;
import com.mercadosolidario.dto.family.FamilyMemberResponse;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.FamilyMember;
import com.mercadosolidario.repository.FamilyMemberRepository;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.util.CpfValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/families/{familyId}/members")
public class FamilyMemberController {

    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository familyRepository;

    public FamilyMemberController(
            FamilyMemberRepository familyMemberRepository,
            FamilyRepository familyRepository) {
        this.familyMemberRepository = familyMemberRepository;
        this.familyRepository = familyRepository;
    }

    @GetMapping
    public ResponseEntity<List<FamilyMemberResponse>> listMembers(@PathVariable Long familyId) {
        requireFamily(familyId);

        List<FamilyMember> members = familyMemberRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        List<FamilyMemberResponse> responses = members.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<FamilyMemberResponse> getMember(
            @PathVariable Long familyId,
            @PathVariable Long memberId) {
        FamilyMember member = requireMemberInFamily(familyId, memberId);
        return ResponseEntity.ok(mapToResponse(member));
    }

    @PostMapping
    public ResponseEntity<FamilyMemberResponse> addMember(
            @PathVariable Long familyId,
            @Valid @RequestBody FamilyMemberRequest request) {
        Family family = requireFamily(familyId);

        FamilyMember member = new FamilyMember();
        member.setFamily(family);
        member.setName(request.getName());
        member.setCpf(request.getCpf() != null ? CpfValidator.unformat(request.getCpf()) : null);
        member.setBirthDate(request.getBirthDate());
        member.setRelationship(request.getRelationship());
        member.setObservations(request.getObservations());

        FamilyMember saved = familyMemberRepository.save(member);
        refreshDependents(family);

        return ResponseEntity.ok(mapToResponse(saved));
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<FamilyMemberResponse> updateMember(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            @Valid @RequestBody FamilyMemberRequest request) {
        FamilyMember member = requireMemberInFamily(familyId, memberId);

        member.setName(request.getName());
        member.setCpf(request.getCpf() != null ? CpfValidator.unformat(request.getCpf()) : null);
        member.setBirthDate(request.getBirthDate());
        member.setRelationship(request.getRelationship());
        member.setObservations(request.getObservations());

        FamilyMember saved = familyMemberRepository.save(member);

        return ResponseEntity.ok(mapToResponse(saved));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable Long familyId,
            @PathVariable Long memberId) {
        FamilyMember member = requireMemberInFamily(familyId, memberId);

        Family family = member.getFamily();
        familyMemberRepository.delete(member);
        refreshDependents(family);

        return ResponseEntity.noContent().build();
    }

    private Family requireFamily(Long familyId) {
        return familyRepository.findById(familyId)
            .orElseThrow(() -> new ResourceNotFoundException("Família não encontrada"));
    }

    private FamilyMember requireMemberInFamily(Long familyId, Long memberId) {
        requireFamily(familyId);
        FamilyMember member = familyMemberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));

        if (!member.getFamily().getId().equals(familyId)) {
            throw new ResourceNotFoundException("Membro não pertence a esta família");
        }

        return member;
    }

    private void refreshDependents(Family family) {
        int count = familyMemberRepository.findByFamilyId(family.getId()).size();
        family.setDependents(count);
        familyRepository.save(family);
    }

    private FamilyMemberResponse mapToResponse(FamilyMember member) {
        return FamilyMemberResponse.builder()
            .id(member.getId())
            .name(member.getName())
            .cpf(member.getCpf() != null ? CpfValidator.format(member.getCpf()) : null)
            .birthDate(member.getBirthDate())
            .relationship(member.getRelationship())
            .observations(member.getObservations())
            .createdAt(member.getCreatedAt())
            .updatedAt(member.getUpdatedAt())
            .build();
    }
}
