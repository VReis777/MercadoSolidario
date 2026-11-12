package com.mercadosolidario.config;

import com.mercadosolidario.model.RunRegistration;
import com.mercadosolidario.repository.RunRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Preenche access_key nas inscricoes antigas (geradas antes da V39). */
@Component
@Order(10)
@RequiredArgsConstructor
public class RunRegistrationAccessKeySetup implements CommandLineRunner {

    private final RunRegistrationRepository registrationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        registrationRepository.findAll().stream()
            .filter(r -> r.getAccessKey() == null)
            .forEach(r -> r.setAccessKey(UUID.randomUUID().toString().replace("-", "")));
    }
}
