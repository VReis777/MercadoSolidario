package com.mercadosolidario.config;

import com.mercadosolidario.model.Role;
import com.mercadosolidario.repository.RoleRepository;
import com.mercadosolidario.repository.UserRepository;
import com.mercadosolidario.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o administrador inicial uma única vez, a partir de INITIAL_ADMIN_EMAIL / INITIAL_ADMIN_NAME.
 * Usa a senha temporária padrão com troca obrigatória; se a conta já existir, nada é alterado.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class InitialAdminSetup implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserAccountService userAccountService;

    @Value("${app.initial-admin.email:}")
    private String email;

    @Value("${app.initial-admin.name:}")
    private String name;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = UserAccountService.normalizeEmail(email);
        if (userRepository.existsByEmail(normalized) || userRepository.existsByUsername(normalized)) {
            log.info("Administrador inicial já existe; nenhuma alteração realizada.");
            return;
        }
        if (name == null || name.isBlank()) {
            log.warn("INITIAL_ADMIN_NAME não informado; administrador inicial não foi criado.");
            return;
        }
        Role admin = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Perfil ADMIN não encontrado no banco."));
        userAccountService.createWithTemporaryPassword(name, normalized, admin);
        log.info("Administrador inicial criado com senha temporária e troca obrigatória no primeiro acesso.");
    }
}
