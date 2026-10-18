package com.mercadosolidario.config;

import com.mercadosolidario.model.Role;
import com.mercadosolidario.model.User;
import com.mercadosolidario.repository.RoleRepository;
import com.mercadosolidario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

@Component
@Profile("local-demo")
@RequiredArgsConstructor
@Slf4j
public class LocalDemoAdminSetup implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;
    private final Environment environment;

    @Override
    @Transactional
    public void run(String... args) throws SQLException {
        if (!"true".equals(System.getenv("ENABLE_LOCAL_DEMO_AUTH"))) {
            log.info("Local demo authentication is disabled.");
            return;
        }

        validateLocalEnvironment();

        String username = requiredEnvironmentVariable("LOCAL_DEMO_ADMIN_USERNAME");
        String password = requiredEnvironmentVariable("LOCAL_DEMO_ADMIN_PASSWORD");
        String fullName = requiredEnvironmentVariable("LOCAL_DEMO_ADMIN_FULL_NAME");
        if (username.length() > 100
                || !username.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
                || password.length() < 8
                || fullName.length() > 255) {
            throw new IllegalStateException("Local demo credentials do not meet the required format.");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("Local demo username already exists; refusing to reuse unknown credentials.");
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("The migrated ADMIN role is unavailable."));

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setEmail(username);
        admin.setFullName(fullName);
        admin.setActive(true);
        
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);

        userRepository.save(admin);
        log.info("Temporary local demo ADMIN account created in the isolated in-memory database.");
    }

    private void validateLocalEnvironment() throws SQLException {
        for (String profile : environment.getActiveProfiles()) {
            if (Set.of("prod", "production", "staging").contains(profile.toLowerCase())) {
                throw new IllegalStateException("Local demo authentication is forbidden with a production-like profile.");
            }
        }
        String address = environment.getProperty("server.address", "");
        if (!"127.0.0.1".equals(address)) {
            throw new IllegalStateException("Local demo authentication requires the server to bind only to 127.0.0.1.");
        }
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            if (!"jdbc:h2:mem:mercado_solidario_local_demo".equals(jdbcUrl)) {
                throw new IllegalStateException("Local demo authentication requires its dedicated in-memory H2 database.");
            }
        }
    }

    private String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required local demo authentication environment is missing: " + name);
        }
        return value;
    }
}
