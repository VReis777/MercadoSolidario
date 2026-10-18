package com.mercadosolidario.service;

import com.mercadosolidario.dto.servicerequest.WelcomeSlotRequest;
import com.mercadosolidario.dto.servicerequest.WelcomeSlotResponse;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.WelcomeAvailability;
import com.mercadosolidario.repository.WelcomeAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Agenda de acolhimento: até 3 dias ativos oferecidos no formulário público
 * para a pessoa escolher quando conversar com a equipe.
 */
@Service
@RequiredArgsConstructor
public class WelcomeAvailabilityService {

    public static final int MAX_ACTIVE_SLOTS = 3;

    private final WelcomeAvailabilityRepository repository;
    private final AuditService auditService;

    /** Datas oferecidas ao público: ativas e a partir de hoje. */
    @Transactional(readOnly = true)
    public List<WelcomeSlotResponse> publicSlots() {
        LocalDate today = LocalDate.now();
        return repository.findByActiveTrueOrderByAvailableDateAsc().stream()
            .filter(slot -> !slot.getAvailableDate().isBefore(today))
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<WelcomeSlotResponse> listAll() {
        return repository.findAll().stream()
            .sorted(java.util.Comparator.comparing(WelcomeAvailability::getAvailableDate))
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public WelcomeSlotResponse create(WelcomeSlotRequest request) {
        if (request.availableDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de acolhimento não pode ser no passado.");
        }
        if (repository.existsByAvailableDate(request.availableDate())) {
            throw new IllegalArgumentException("Já existe um dia de acolhimento cadastrado nessa data.");
        }
        if (repository.countByActiveTrue() >= MAX_ACTIVE_SLOTS) {
            throw new IllegalArgumentException(
                "São permitidos até " + MAX_ACTIVE_SLOTS + " dias ativos de acolhimento. Desative um dia antes de adicionar outro.");
        }
        WelcomeAvailability slot = new WelcomeAvailability();
        slot.setAvailableDate(request.availableDate());
        slot.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        slot.setActive(true);
        WelcomeAvailability saved = repository.save(slot);
        auditService.log("WELCOME_SLOT_CREATED", "WelcomeAvailability", saved.getId(),
            "Dia de acolhimento: " + saved.getAvailableDate());
        return toResponse(saved);
    }

    @Transactional
    public WelcomeSlotResponse setActive(Long id, boolean active) {
        WelcomeAvailability slot = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Dia de acolhimento não encontrado"));
        if (active && !Boolean.TRUE.equals(slot.getActive())
                && repository.countByActiveTrue() >= MAX_ACTIVE_SLOTS) {
            throw new IllegalArgumentException(
                "São permitidos até " + MAX_ACTIVE_SLOTS + " dias ativos de acolhimento.");
        }
        slot.setActive(active);
        WelcomeAvailability saved = repository.save(slot);
        auditService.log(active ? "WELCOME_SLOT_ACTIVATED" : "WELCOME_SLOT_DEACTIVATED",
            "WelcomeAvailability", id, "Dia de acolhimento: " + saved.getAvailableDate());
        return toResponse(saved);
    }

    /** Confere se a data escolhida pelo público corresponde a um dia ativo e vigente. */
    @Transactional(readOnly = true)
    public boolean isAvailableForChoice(LocalDate date) {
        return date != null
            && !date.isBefore(LocalDate.now())
            && repository.existsByAvailableDateAndActiveTrue(date);
    }

    private WelcomeSlotResponse toResponse(WelcomeAvailability slot) {
        return new WelcomeSlotResponse(slot.getId(), slot.getAvailableDate(), slot.getNote(), slot.getActive());
    }
}
