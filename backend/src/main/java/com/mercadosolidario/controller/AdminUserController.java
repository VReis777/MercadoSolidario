package com.mercadosolidario.controller;

import com.mercadosolidario.dto.auth.CreateUserInvitationRequest;
import com.mercadosolidario.dto.auth.UserInvitationResponse;
import com.mercadosolidario.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import com.mercadosolidario.dto.auth.UpdateUserRoleRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAccountService userAccountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserInvitationResponse invite(
            @Valid @RequestBody CreateUserInvitationRequest request,
            Authentication authentication) {
        return userAccountService.invite(request, authentication.getName());
    }

    /** Troca a autoridade de uma pessoa (só o dono, inclusive promover a ADMIN). */
    @PatchMapping("/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication) {
        userAccountService.changeRole(id, request.role(), authentication.getName());
    }
}
