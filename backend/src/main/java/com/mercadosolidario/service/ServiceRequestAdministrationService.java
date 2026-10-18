package com.mercadosolidario.service;

import com.mercadosolidario.dto.servicerequest.*;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.*;
import com.mercadosolidario.repository.PublicServiceRequestHistoryRepository;
import com.mercadosolidario.repository.PublicServiceRequestRepository;
import com.mercadosolidario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceRequestAdministrationService {
    private final PublicServiceRequestRepository requests;
    private final PublicServiceRequestHistoryRepository history;
    private final UserRepository users;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ServiceRequestListResponse> list(
            ServiceRequestStatus status, LocalDate fromDate, LocalDate toDate, String protocol, Pageable pageable,
            Authentication authentication) {
        validateDateRange(fromDate, toDate);
        Pageable bounded = pageable.getPageSize() > 100
            ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
            : pageable;
        String protocolHash = protocol == null || protocol.isBlank() ? null : ServiceRequestProtocol.hash(protocol);
        String assignedUsername = isLimitedDeacon(authentication) ? authentication.getName() : null;
        // Specification evita o problema do PostgreSQL de não inferir o tipo de parâmetros nulos em JPQL.
        var spec = org.springframework.data.jpa.domain.Specification
            .<PublicServiceRequest>where(null)
            .and(status == null ? null : (root, q, cb) -> cb.equal(root.get("status"), status))
            .and(fromDate == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()))
            .and(toDate == null ? null : (root, q, cb) -> cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()))
            .and(protocolHash == null ? null : (root, q, cb) -> cb.equal(root.get("protocolHash"), protocolHash))
            .and(assignedUsername == null ? null : (root, q, cb) -> cb.equal(root.get("assignedUser").get("username"), assignedUsername));
        return requests.findAll(spec, bounded).map(request -> toListResponse(request, authentication));
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse get(Long id, Authentication authentication) {
        return toResponse(find(id, authentication), authentication);
    }

    @Transactional
    public ServiceRequestResponse changeStatus(Long id, ServiceRequestStatus target, String observation,
                                               Authentication authentication) {
        PublicServiceRequest request = find(id, authentication);
        ServiceRequestStatus previous = request.getStatus();
        if (!isValidTransition(previous, target)) {
            throw new IllegalArgumentException("Transição de status inválida");
        }
        boolean admin = hasRole(authentication, "ADMIN");
        if (!admin && hasRole(authentication, "RH_TESOURARIA")
                && !(previous == ServiceRequestStatus.EM_ANALISE && target == ServiceRequestStatus.ENCAMINHADA)) {
            throw new org.springframework.security.access.AccessDeniedException("Transição não permitida");
        }
        if (!admin && hasRole(authentication, "DIACONO")
                && !(previous == ServiceRequestStatus.RECEBIDA && target == ServiceRequestStatus.EM_ANALISE)) {
            throw new org.springframework.security.access.AccessDeniedException("Transição não permitida");
        }

        request.setStatus(target);
        requests.save(request);
        String note = usableObservation(observation,
            "Status alterado de " + previous + " para " + target + ".");
        record(request, "STATUS", previous, target, actor(authentication), note);
        auditService.log("SERVICE_REQUEST_STATUS_CHANGED", "PublicServiceRequest", id,
            previous + " -> " + target);
        return toResponse(request, authentication);
    }

    @Transactional
    public ServiceRequestResponse addNote(Long id, String note, Authentication authentication) {
        PublicServiceRequest request = find(id, authentication);
        record(request, "NOTE", null, null, actor(authentication), note.trim());
        auditService.log("SERVICE_REQUEST_NOTE_ADDED", "PublicServiceRequest", id, "Nota interna adicionada");
        return toResponse(request, authentication);
    }

    @Transactional
    public ServiceRequestResponse assign(Long id, String username, String observation,
                                         Authentication authentication) {
        PublicServiceRequest request = find(id, authentication);
        User assignee = users.findByUsername(username.trim())
            .filter(user -> Boolean.TRUE.equals(user.getActive()))
            .orElseThrow(() -> new IllegalArgumentException("Usuário de destino inválido"));
        Set<String> roleNames = assignee.getRoles().stream().map(Role::getName)
            .collect(java.util.stream.Collectors.toSet());
        if (roleNames.stream().noneMatch(Set.of("ADMIN", "RH_TESOURARIA", "DIACONO")::contains)) {
            throw new IllegalArgumentException("Usuário de destino sem função operacional");
        }
        String previous = request.getAssignedUser() == null ? null : request.getAssignedUser().getUsername();
        request.setAssignedUser(assignee);
        requests.save(request);
        String fallback = previous == null
            ? "Solicitação atribuída a " + assignee.getUsername() + "."
            : "Atribuição alterada de " + previous + " para " + assignee.getUsername() + ".";
        record(request, "ASSIGN", null, null, actor(authentication), usableObservation(observation, fallback));
        auditService.log("SERVICE_REQUEST_ASSIGNED", "PublicServiceRequest", id,
            "Solicitação encaminhada para " + assignee.getUsername());
        return toResponse(request, authentication);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestHistoryResponse> getHistory(Long id, Authentication authentication) {
        find(id, authentication);
        boolean admin = hasRole(authentication, "ADMIN");
        return history.findByServiceRequestIdOrderByCreatedAtAscIdAsc(id).stream()
            .filter(entry -> admin || !"NOTE".equals(entry.getAction()))
            .map(entry -> new ServiceRequestHistoryResponse(
                entry.getId(),
                entry.getOldStatus(),
                entry.getNewStatus(),
                entry.getActorUsername(),
                entry.getCreatedAt(),
                admin ? entry.getObservation() : null
            )).toList();
    }

    private ServiceRequestResponse toResponse(PublicServiceRequest request, Authentication authentication) {
        boolean admin = hasRole(authentication, "ADMIN");
        boolean assigned = isAssignee(request, authentication);
        // Contato real e dados da família só para ADMIN ou para o responsável atribuído.
        boolean canSeeSensitive = admin || assigned;
        boolean diacono = hasRole(authentication, "DIACONO") && !admin;
        return new ServiceRequestResponse(
            request.getId(),
            request.getStatus(),
            request.getCreatedAt(),
            request.getUpdatedAt(),
            request.getAssignedUser() == null ? null : request.getAssignedUser().getUsername(),
            canSeeSensitive
                ? request.getContactValue()
                : PublicServiceRequestService.maskContact(request.getContactValue(), request.getContactPreference()),
            canSeeSensitive ? request.getLocation() : (diacono ? null : request.getLocation()),
            canSeeSensitive ? request.getHouseholdSize() : (diacono ? null : request.getHouseholdSize()),
            request.getAssistanceType(),
            canSeeSensitive ? request.getFullName() : (diacono ? null : request.getFullName()),
            canSeeSensitive
                ? (request.getContactPreference() == null ? null : request.getContactPreference().name())
                : (diacono || request.getContactPreference() == null ? null : request.getContactPreference().name()),
            admin ? request.getMessage() : null,
            request.getAssignedUser() == null ? null : request.getAssignedUser().getId(),
            request.getWelcomePreference() == null ? null : request.getWelcomePreference().name(),
            request.getWelcomeDate(),
            request.getWelcomeTime()
        );
    }

    private ServiceRequestListResponse toListResponse(
            PublicServiceRequest request, Authentication authentication) {
        boolean diacono = hasRole(authentication, "DIACONO") && !hasRole(authentication, "ADMIN");
        boolean canSeeContact = hasRole(authentication, "ADMIN") || isAssignee(request, authentication);
        return new ServiceRequestListResponse(
            request.getId(),
            request.getStatus(),
            request.getCreatedAt(),
            request.getUpdatedAt(),
            request.getAssignedUser() == null ? null : request.getAssignedUser().getUsername(),
            canSeeContact
                ? request.getContactValue()
                : PublicServiceRequestService.maskContact(request.getContactValue(), request.getContactPreference()),
            diacono ? null : request.getLocation(),
            diacono ? null : request.getHouseholdSize(),
            request.getAssistanceType(),
            request.getWelcomePreference() == null ? null : request.getWelcomePreference().name(),
            request.getWelcomeDate(),
            request.getWelcomeTime()
        );
    }

    private PublicServiceRequest find(Long id, Authentication authentication) {
        return requests.findById(id)
            .filter(request -> !isLimitedDeacon(authentication)
                || request.getAssignedUser() != null
                    && authentication.getName().equals(request.getAssignedUser().getUsername()))
            .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
    }

    private boolean isLimitedDeacon(Authentication authentication) {
        return hasRole(authentication, "DIACONO") && !hasRole(authentication, "ADMIN");
    }

    private boolean isAssignee(PublicServiceRequest request, Authentication authentication) {
        return authentication != null
            && request.getAssignedUser() != null
            && authentication.getName().equals(request.getAssignedUser().getUsername());
    }

    private void record(PublicServiceRequest request, String action, ServiceRequestStatus previous,
                        ServiceRequestStatus next, String actor, String observation) {
        PublicServiceRequestHistory entry = new PublicServiceRequestHistory();
        entry.setServiceRequest(request);
        entry.setAction(action);
        entry.setOldStatus(previous);
        entry.setNewStatus(next);
        entry.setActorUsername(actor);
        entry.setObservation(observation);
        history.save(entry);
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "unknown" : authentication.getName();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> authority.equals("ROLE_" + role));
    }

    private boolean isValidTransition(ServiceRequestStatus previous, ServiceRequestStatus target) {
        if (previous == null || target == null || previous == target) {
            return false;
        }
        return switch (previous) {
            case RECEBIDA -> target == ServiceRequestStatus.EM_ANALISE;
            case EM_ANALISE -> target == ServiceRequestStatus.ENCAMINHADA
                || target == ServiceRequestStatus.AGUARDANDO_CONTATO
                || target == ServiceRequestStatus.CANCELADA;
            case AGUARDANDO_CONTATO -> target == ServiceRequestStatus.ATENDIDA
                || target == ServiceRequestStatus.ENCAMINHADA
                || target == ServiceRequestStatus.CANCELADA;
            case ENCAMINHADA -> target == ServiceRequestStatus.ATENDIDA
                || target == ServiceRequestStatus.CANCELADA;
            case ATENDIDA, CANCELADA -> false;
        };
    }

    private String usableObservation(String observation, String fallback) {
        return observation == null || observation.isBlank() ? fallback : observation.trim();
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("A data inicial deve ser anterior ou igual à data final");
        }
    }
}
