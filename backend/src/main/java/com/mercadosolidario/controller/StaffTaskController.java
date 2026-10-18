package com.mercadosolidario.controller;

import com.mercadosolidario.dto.task.AssignableUserResponse;
import com.mercadosolidario.dto.task.CreateStaffTaskRequest;
import com.mercadosolidario.dto.task.StaffTaskResponse;
import com.mercadosolidario.service.StaffTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class StaffTaskController {

    private final StaffTaskService staffTaskService;

    @GetMapping
    public List<StaffTaskResponse> listMine(Authentication authentication) {
        return staffTaskService.listVisibleTo(authentication.getName());
    }

    @GetMapping("/assignable-users")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public List<AssignableUserResponse> assignableUsers() {
        return staffTaskService.listAssignableUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH_TESOURARIA')")
    public StaffTaskResponse create(@Valid @RequestBody CreateStaffTaskRequest request,
                                    Authentication authentication) {
        return staffTaskService.create(request, authentication.getName());
    }

    @PatchMapping("/{id}/complete")
    public StaffTaskResponse complete(@PathVariable Long id, Authentication authentication) {
        return staffTaskService.complete(id, authentication.getName());
    }
}
