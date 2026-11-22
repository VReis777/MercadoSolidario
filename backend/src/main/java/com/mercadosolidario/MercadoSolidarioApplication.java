package com.mercadosolidario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class MercadoSolidarioApplication {

    public static void main(String[] args) {
        // Horário de Brasília em qualquer ambiente (local, Render, Docker).
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        SpringApplication.run(MercadoSolidarioApplication.class, args);
    }
}
