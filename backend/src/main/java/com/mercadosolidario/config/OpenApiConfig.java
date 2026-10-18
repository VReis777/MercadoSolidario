package com.mercadosolidario.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mercadoSolidarioOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("Development Server");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.mercadosolidario.com");
        prodServer.setDescription("Production Server");

        Contact contact = new Contact();
        contact.setEmail("contato@mercadosolidario.com");
        contact.setName("Mercado Solidário");

        Info info = new Info()
            .title("Mercado Solidário API")
            .version("1.0.0")
            .description("API REST para sistema de gestão social Mercado Solidário - Natal com Dignidade")
            .contact(contact);

        return new OpenAPI()
            .info(info)
            .servers(List.of(devServer, prodServer));
    }
}
