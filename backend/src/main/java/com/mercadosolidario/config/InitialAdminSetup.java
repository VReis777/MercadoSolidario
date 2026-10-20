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

    @Value("${app.initial-admin2.email:}")
    private String secondEmail;

    @Value("${app.initial-admin2.name:}")
    private String secondName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing(email, name);
        createIfMissing(secondEmail, secondName);
    }

    private void createIfMissing(String rawEmail, String fullName) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return;
        }
        String normalized = UserAccountService.normalizeEmail(rawEmail);
        if (userRepository.existsByEmail(normalized) || userRepository.existsByUsername(normalized)) {
            log.info("Administrador {} já existe; nenhuma alteração realizada.", normalized);
            return;
        }
        if (fullName == null || fullName.isBlank()) {
            log.warn("Nome do administrador {} não informado; conta não foi criada.", normalized);
            return;
        }
        Role admin = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Perfil ADMIN não encontrado no banco."));
        userAccountService.createWithTemporaryPassword(fullName, normalized, admin);
        log.info("Administrador {} criado com senha temporária e troca obrigatória no primeiro acesso.", normalized);
    }
}
