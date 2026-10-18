package com.mercadosolidario.service;

import com.mercadosolidario.dto.task.AssignableUserResponse;
import com.mercadosolidario.dto.task.CreateStaffTaskRequest;
import com.mercadosolidario.dto.task.StaffTaskResponse;
import com.mercadosolidario.exception.ResourceNotFoundException;
import com.mercadosolidario.model.Role;
import com.mercadosolidario.model.StaffTask;
import com.mercadosolidario.model.User;
import com.mercadosolidario.repository.StaffTaskRepository;
import com.mercadosolidario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffTaskService {

    private final StaffTaskRepository staffTaskRepository;
    private final UserRepository userRepository;
    private final InvitationEmailSender emailSender;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AssignableUserResponse> listAssignableUsers() {
        return userRepository.findAll(Sort.by("fullName")).stream()
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .map(user -> new AssignableUserResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRoles().stream().map(Role::getName).sorted().toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffTaskResponse> listVisibleTo(String username) {
        User user = requireUser(username);
        return staffTaskRepository.findVisibleTo(user.getId()).stream()
                .map(task -> toResponse(task, false))
                .toList();
    }

    @Transactional
    public StaffTaskResponse create(CreateStaffTaskRequest request, String creatorUsername) {
        User creator = requireUser(creatorUsername);
        User assignee = userRepository.findById(request.getAssigneeId())
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa responsável não encontrada ou inativa."));

        StaffTask task = new StaffTask();
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription().trim() : null);
        task.setAssignee(assignee);
        task.setCreatedBy(creator);
        StaffTask saved = staffTaskRepository.save(task);

        boolean emailSent = trySendAssignmentEmail(saved, creator, assignee);
        auditService.log("TAREFA_CRIADA", "staff_task", saved.getId(),
                "Tarefa atribuída a %s por %s; e-mail %s.".formatted(
                        assignee.getEmail(), creator.getEmail(), emailSent ? "enviado" : "não enviado"));
        return toResponse(saved, emailSent);
    }

    @Transactional
    public StaffTaskResponse complete(Long taskId, String username) {
        User user = requireUser(username);
        StaffTask task = staffTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));
        boolean isAssignee = task.getAssignee().getId().equals(user.getId());
        boolean isCreator = task.getCreatedBy().getId().equals(user.getId());
        if (!isAssignee && !isCreator) {
            throw new IllegalArgumentException("Somente o responsável ou quem criou a tarefa pode concluí-la.");
        }
        task.setStatus("CONCLUIDA");
        task.setCompletedAt(LocalDateTime.now());
        StaffTask saved = staffTaskRepository.save(task);
        auditService.log("TAREFA_CONCLUIDA", "staff_task", saved.getId(),
                "Tarefa concluída por %s.".formatted(user.getEmail()));
        return toResponse(saved, false);
    }

    private boolean trySendAssignmentEmail(StaffTask task, User creator, User assignee) {
        try {
            emailSender.sendWithCopy(
                    assignee.getEmail(),
                    creator.getEmail(),
                    "Nova tarefa para você - Mercado Solidário",
                    """
                    Olá, %s.

                    %s atribuiu uma tarefa para você no Mercado Solidário:

                    Tarefa: %s
                    %s
                    Quem pediu recebeu uma cópia deste e-mail. Você pode marcar a tarefa como concluída na área de usuários do sistema.
                    """.formatted(
                            assignee.getFullName(),
                            creator.getFullName(),
                            task.getTitle(),
                            task.getDescription() != null ? "Detalhes: " + task.getDescription() + "\n" : ""));
            return true;
        } catch (RuntimeException exception) {
            log.warn("Task {} created but assignment e-mail was not sent.", task.getId());
            return false;
        }
    }

    private StaffTaskResponse toResponse(StaffTask task, boolean emailSent) {
        return new StaffTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getAssignee().getId(),
                task.getAssignee().getFullName(),
                task.getAssignee().getEmail(),
                task.getCreatedBy().getFullName(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getCompletedAt(),
                emailSent);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }
}
