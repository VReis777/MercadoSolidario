package com.mercadosolidario.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class InvitationMailConfiguration {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    @ConditionalOnExpression(
        "'${SMTP_HOST:${spring.mail.host:}}'.trim().length() > 0")
    public JavaMailSender invitationJavaMailSender(Environment environment) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(environment.getProperty("SMTP_HOST",
            environment.getRequiredProperty("spring.mail.host")));
        Integer port = environment.getProperty("SMTP_PORT", Integer.class);
        if (port == null) {
            port = environment.getProperty("spring.mail.port", Integer.class, 587);
        }
        sender.setPort(port);
        sender.setUsername(environment.getProperty("SMTP_USERNAME",
            environment.getProperty("spring.mail.username", "")));
        sender.setPassword(environment.getProperty("SMTP_PASSWORD",
            environment.getProperty("spring.mail.password", "")));

        Properties mailProperties = sender.getJavaMailProperties();
        boolean hasUsername = !sender.getUsername().isBlank();
        boolean hasPassword = !sender.getPassword().isBlank();
        if (hasUsername != hasPassword) {
            throw new IllegalStateException("SMTP_USERNAME and SMTP_PASSWORD must be configured together.");
        }
        mailProperties.put("mail.smtp.auth", Boolean.toString(hasUsername));
        mailProperties.put("mail.smtp.starttls.enable", environment.getProperty("SMTP_STARTTLS", "true"));
        mailProperties.put("mail.smtp.connectiontimeout", "5000");
        mailProperties.put("mail.smtp.timeout", "5000");
        mailProperties.put("mail.smtp.writetimeout", "5000");
        return sender;
    }
}
